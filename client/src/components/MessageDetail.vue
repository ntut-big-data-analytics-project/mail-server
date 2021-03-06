/*******************************************************************************
 * Copyright 2017 - 2018 Sparta Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

<template>
  <div id="message-detail">
    <b-alert id="forward-alert" :show="dismissCountDown" variant="success" @dismiss-count-down="countDownChanged">
      Mail {{ message.messageId }} successfully sent to <strong>{{ forwardRecipient }}</strong>
    </b-alert>

    <b-alert id="forward-error" :show="validationError" variant="danger">
      {{errors.first('forwardEmail')}}
    </b-alert>
    <b-card header-tag="header" class="details-container">
      <div slot="header" class="d-flex flex-row p-1" :class="{
      'bg-danger spam': message.isSpam===true
      }">
        <router-link to="/" class="d-flex">
          <b-button id="backButton" size="sm" variant="light">
            <span class="fa fa-arrow-left" aria-hidden="true"></span>
          </b-button>
        </router-link>
        <h4 class="pl-2 message-subject">{{ message.subject }}</h4>
        <div>
        <!--<div class="forward-group input-group">-->
          <!--<input v-model="forwardRecipient" v-validate="'required|email'"-->
            <!--type="email" name="forwardEmail" placeholder="Forward to..." class="form-control"></input>-->
          <!--<b-button id="fwdButton" size="sm" variant="primary" @click="forwardMail">-->
            <!--Forward-->
            <!--<span class="fa fa-forward" aria-hidden="true"></span>-->
          <!--</b-button>-->
        <!--</div>-->
      </div>
      </div>
      <table class="table table-sm table-condensed addresses">
          <tr>
            <td><strong>From:</strong></td>
            <td class="message-sender">{{message.senderEmail}}</td>
            <td class="message-received-date text-right pr-2">{{ receivedDate | date('%b %-d, %Y %r') }}</td>
          </tr>
          <tr>
            <td><strong>To:</strong></td>
            <td class="message-recipients" colspan="2">{{message.recipients}}</td>
          </tr>
          <tr class="message-attach-row" v-show="attachmentList.length !== 0">
            <td><strong>Atts:</strong></td>
            <td colspan="2">
              <ul class="attach-list">
                <li class="attach-item" v-for="attachment in attachmentList">
                  <b-link class="attach-link" :href="attachment.downloadURI" dow><span class="attach-icon fa fa-paperclip" aria-hidden="true"></span><span class="attach-link-text">{{attachment.filename}}</span></b-link>
                  <span class="attach-size">({{attachment.size | prettyBytes}})</span>
                </li>
              </ul>
            </td>
          </tr>
        <tr class="message-action">
          <td colspan="3" class="text-right">
            <button class="btn btn-outline-danger" v-if="!message.isSpam" @click="updateIsSpam(true)">這是垃圾信</button>
            <button class="btn btn-outline-dark" v-if="message.isSpam" @click="updateIsSpam(false)">這是不是垃圾信</button>
          </td>
        </tr>
      </table>
      <b-tabs :no-fade="true" ref="tabs">
        <b-tab id="html-body" title="HTML" :disabled="!message.messageHasBodyHTML">
            <!-- <mail-metadata :message="message"></mail-metadata> -->
            <iframe class="mail-summary-content mail-summary-content-html" :srcdoc="messageHTML"></iframe>
        </b-tab>
        <b-tab id="html-text" title="Text" :disabled="!message.messageHasBodyText">
            <!-- <mail-metadata :message="message"></mail-metadata> -->
            <div class="mail-summary-content mail-summary-content-pre">{{message.messageBodyText}}</div>
        </b-tab>
        <b-tab id="original-message" title="Raw">
          <div class="mail-summary-content mail-summary-content-pre">{{rawText}}</div>
        </b-tab>
      </b-tabs>
    </b-card>
  </div>
</template>

<script>
import Vue from 'vue'
import VeeValidate from 'vee-validate'
import BootstrapVue from 'bootstrap-vue'
import messagesApi from '@/api/messages'
import filters from '@/filters/filters'

Vue.use(BootstrapVue)
Vue.use(VeeValidate)

export default {
  name: 'message-detail',
  data () {
    return {
      message: {},
      dismissCountDown: null,
      showForwarding: false,
      busyForwarding: false,
      forwardRecipient: '',
      errorContent: null,
      validationError: false,
      rawText: '',
    }
  },
  filters: {
    prettyBytes: filters.prettyBytes
  },
  mounted () {
    this.loadMessage()
  },
  watch: {
    message () {
      let selectedTab = this.message.messageHasBodyHTML ? 0 : this.message.messageHasBodyText ? 1 : 2
      this.setTab(selectedTab)
    },
    forwardRecipient () {
      this.validationError = false
    }
  },
  computed: {
    messageHTML () {
      if (this.message && this.message.messageHasBodyHTML) {
        var html = this.message.messageBodyHTML
        var el = document.createElement('html')

        el.innerHTML = html

        var links = el.getElementsByTagName('a')

        for (var i = 0; i < links.length; i++) {
          links[i].setAttribute('target', '_new')
        }

        return el.innerHTML
      } else {
        return ''
      }
    },
    receivedDate () {
      const receivedDate = this.message && this.message.receivedDate
      return receivedDate || 0
    },
    messageRawEndpoint () {
      if (this.message.messageId) {
        return messagesApi.getMessageRAWEndpoint(this.message.messageId)
      } else {
        return ''
      }
    },
    attachmentList () {
      return ((this.message && this.message.attachments) ? this.message.attachments : [])
        .filter(f => f.disposition === 'attachment') // drop any 'inline' attachments
        .map(x => Object.assign(x, {
          'downloadURI': '/rest/messages/' + this.message.messageId + '/att/' + x.attachmentId
        }))
    }
  },
  methods: {
    loadMessage(){
      const messageId = this.$route.params.messageId

      messagesApi.getMessageDetail(messageId)
        .then((response) => {
          this.message = response.data
        })
        .catch(() => {
          console.log('Service failed to query message detail')
        });
      messagesApi.getMessageRaw(messageId)
        .then((response) => {
          this.rawText = response.data
        })
        .catch(() => {
          console.log('Service failed to query message detail')
        });
    },
    forwardMail () {
      this.$validator.validateAll()
        .then(() => {
          if (this.errors.has('forwardEmail')) {
            this.validationError = true
          } else {
            this.busyForwarding = true

            messagesApi.forwardMessage(this.message.messageId, this.forwardRecipient)
              .then((response) => {
                this.busyForwarding = false
                this.dismissCountDown = 5
              })
              .catch(() => {
                console.log('Service failed to forward message to ' + this.forwardRecipient)
              })
          }
        })
    },
    countDownChanged (dismissCountDown) {
      this.dismissCountDown = dismissCountDown
    },
    updateIsSpam(val){
      messagesApi.updateIsSpam(this.message.messageId, val)
        .then(r => {
          this.loadMessage();
        });
    },
    setTab (index) {
      this.$nextTick(() => {
        this.$refs.tabs.setTab(index, true)
      })
    }
  }
}
</script>

<style scoped lang="less">

.message-subject {
  flex-grow: 1;
  margin-bottom: 0;
  line-height: 1.6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.forward-group {
  input {
    min-width: 200px;
  }
}

.nav.nav-tabs {
  padding-left: 3px;
}

.details-container {
  .card-header {
    padding: 0;
    color: #31708f;
    background-color: #d9edf7;
    border-color: #bce8f1;
  }
  .card-body {
    padding: 0;
  }
}

table.addresses {
  margin-bottom: 0;
}

.mail-summary {
    height: 500px;
    flex-direction: column;
}

.mail-summary-content {
    width: 100%;
    height: 330px;
    border: 0;
    padding: 5px;
}

.mail-summary-content-pre {
    background-color: #eeeeee;
    white-space: pre;
    font-family: monospace;
    overflow: auto;
}

ul.attach-list {
  padding: 0;
  li {
    display: inline;
    padding: 0 0 0 10px;
  }
  .attach-icon {
    padding-right: 5px;
    color: black;
  }
  .attach-link-text {
    font-size: 0.9em;
  }
  .attach-size {
    font-style: italic;
    font-size: 0.8em;
  }
}
</style>

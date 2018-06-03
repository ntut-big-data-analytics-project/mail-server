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
  <div class="row">
    <div id="mailCriteriaAndResults" class="col-lg-12">
      <div id="mailCriteria" class="mt-4">
        <form>
          <div class="input-group">
            <span class="input-group-addon text-white bg-primary">寄信人</span>

            <input id="mainSearchTxt" type="text" class="form-control"
                   placeholder="asdf@ntut.edu.tw"
                   v-model="recipientEmail"
                   @keyup.enter="clearAndFetchMessages"/>

            <span class="input-group-btn">
                    <button id="mainSearchBut" class="btn btn-success" type="button"
                            @click="clearAndFetchMessages">
                        <span class="glyphicon glyphicon-search" aria-hidden="true"></span>
                      <i class="material-icons">search</i>
                    </button>
                </span>
          </div>
          <div class="row">
            <div class="col">
              <p></p>
            </div>
          </div>
          <div class="row">
            <div class="col">
              <div class="btn-group input-group" role="group" aria-label="...">
                <button type="button" class="btn" @click="changeMode(-1)" :class="{
                  'btn-default': mode!==-1,
                  'btn-primary': mode ===-1
                }">全部
                </button>
                <button type="button" class="btn btn-default" @click="changeMode(0)" :class="{
                  'btn-default': mode!==0,
                  'btn-primary': mode ===0
                }">一般信件
                </button>
                <button type="button" class="btn btn-default" @click="changeMode(1)" :class="{
                  'btn-default': mode!==1,
                  'btn-primary': mode ===1
                }">垃圾信件
                </button>
              </div>
            </div>
            <div class="col text-right">
              <a href="./rest/messages/download" class="btn btn-outline-success">Download All Email</a>
            </div>
          </div>
          <div class="row">
            <div class="col">
              <p></p>
            </div>
          </div>
        </form>
      </div>

      <div id="mailResults">
        <table class="table table-striped table-hover"
               v-infinite-scroll="fetchMessages"
               infinite-scroll-disabled="busy"
               infinite-scroll-distance="10">
          <thead>
          <tr>
            <th width="2%">#</th>
            <th width="15%">From</th>
            <th width="25%">To</th>
            <th width="49%">Subject</th>
            <th width="1%"><span class="attach-icon fa fa-paperclip"></span></th>
            <th width="8%">Time</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="item in items" @click="rowClick(item)" :class="{
            'table-danger': item.isSpam===true
          }">
            <td class="item-id" nowrap>{{ item.messageId }}</td>
            <td class="item-sender-mail" nowrap>{{ item.senderEmail }}</td>
            <td class="item-recipients">{{ item.recipients }}</td>
            <td class="item-subject">{{ item.subject }}</td>
            <td class="item-has-attach"><span class="attach-icon fa fa-paperclip" v-show="item.hasAttachments"></span>
            </td>
            <td class="item-received-date" nowrap>{{ item.receivedDate | date('%b %-d, %Y %r') }}</td>
          </tr>
          </tbody>
        </table>

        <div class="progress" v-show="busy">
          <div class="progress-bar" role="progressbar" aria-valuenow="70"
               aria-valuemin="0" aria-valuemax="100" style="width:100%">
            Fetching...
          </div>
        </div>

        <div class="card bg-faded text-center" v-show="showEmptyMessagesPane">
          <div class="card-block">
            <h3 class="card-title">No Messages</h3>
            <p class="card-text">No messages found for the current criteria - Try sending some to the mail server!</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Vue from 'vue'
import infiniteScroll from 'vue-infinite-scroll'
import filters from 'vue-filter'
import messagesApi from '@/api/messages'

Vue.use(infiniteScroll)
Vue.use(filters)
const MODE = {
  ALL: -1,
  NORMAL: 0,
  SPAM: 1
}
export default {
  name: 'message-table',
  data() {
    return {
      items: [],
      busy: false,
      noMorePages: false,
      page: 0,
      size: 40,
      recipientEmail: '',
      selectedMail: {},
      mode: MODE.ALL
    }
  },
  mounted() {
    this.clearAndFetchMessages()
  },
  computed: {
    showEmptyMessagesPane() {
      return !this.busy && this.items.length < 1
    }
  },
  methods: {
    clearAndFetchMessages() {
      this.items = []
      this.page = 0
      this.noMorePages = false

      this.fetchMessages()
    },

    changeMode(mode) {
      this.mode = mode
      this.clearAndFetchMessages()
    },

    fetchMessages() {
      if (this.busy || this.noMorePages) {
        return
      }

      this.busy = true

      messagesApi.getMessageList(this.size, this.page, this.recipientEmail, this.mode)
        .then((response) => {
          const messages = response.data.messages

          this.items = this.items.concat(messages)
          this.noMorePages = messages.length < 1
          this.busy = false
          this.page++
        })
        .catch(() => {
          this.busy = false
          console.log('Service failed to query message list')
        })
    },
    rowClick(selectedMail) {
      this.$router.push({name: 'MessageDetail', params: {messageId: selectedMail.messageId}})
    }
  }
}
</script>

<style>
#mailResults {
  font-size: 14px;
}

#mailResults .table-hover tbody tr:hover td, .table-hover tbody tr:hover th {
  background-color: #efefff;
  cursor: pointer;
}

</style>


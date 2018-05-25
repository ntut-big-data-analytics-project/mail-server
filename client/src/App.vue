/*******************************************************************************
* Copyright 2017 Sparta Systems, Inc
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
  <div id="app">
    <b-navbar type="light" variant="light" toggleable class="">
      <div class="container">
        <b-nav-toggle target="nav_collapse"/>
        <router-link to="/">
          <a class="navbar-brand p-0">
            <!--<img src="./assets/holdmail-header-logo.png" alt="HoldMail Logo" title="HoldMail Home" height="40">-->
            <span class="logo-text">垃圾郵件攔截系統</span>
          </a>
        </router-link>
        <b-collapse is-nav id="nav_collapse">
          <b-navbar-nav class="ml-auto">
          <b-nav-item href="#">
          <!--<i class="holdmail-nav-link fa fa-github"></i> GitHub-->
            NTUT CSIE BDA Final Project
          </b-nav-item>
          </b-navbar-nav>
        </b-collapse>
      </div>
    </b-navbar>
    <div class="container">
      <router-view/>
    </div>
  </div>
</template>

<script>
import Vue from 'vue'
import BootstrapVue from 'bootstrap-vue'

Vue.use(BootstrapVue)

export default {
  name: 'app'
}
</script>

<style scoped lang="less">
.navbar {
  border-bottom: 1px #e7e7e7 solid;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .15), 0 1px 5px rgba(0, 0, 0, .075);
}
</style>

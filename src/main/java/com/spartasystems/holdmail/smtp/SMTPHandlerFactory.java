/*******************************************************************************
 * Copyright 2016 Sparta Systems, Inc
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

package com.spartasystems.holdmail.smtp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;
import org.subethamail.smtp.MessageContext;

@Component
public class SMTPHandlerFactory implements org.subethamail.smtp.MessageHandlerFactory {

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Value("${holdmail.smtp.hook_url:http://127.0.0.1}")
    private String smtpHookUrl;

    @Override
    public SMTPHandler create(MessageContext ctx) {

        SMTPHandler smtpHandler = new SMTPHandler(ctx, smtpHookUrl);
        beanFactory.autowireBean(smtpHandler);
        return smtpHandler;
    }
}

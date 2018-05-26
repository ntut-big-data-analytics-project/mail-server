/*******************************************************************************
 * Copyright 2016 - 2017 Sparta Systems, Inc
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

import com.spartasystems.holdmail.domain.Message;
import com.spartasystems.holdmail.domain.MessageHeaders;
import com.spartasystems.holdmail.service.MessageService;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.james.mime4j.Charsets;
import org.apache.james.mime4j.codec.DecoderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class SMTPHandler implements MessageHandler {
    private Logger logger = LoggerFactory.getLogger(SMTPHandler.class);

    @Autowired
    private MessageService messageService;

    private byte[] data = {};
    private String senderHost;
    private String senderEmail;
    private List<String> recipients = new ArrayList<>();
    private final String hookUrl;
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    public SMTPHandler() {
        hookUrl = "http://127.0.0.1";
    }

    public SMTPHandler(MessageContext ctx, String hookUrl) {

        InetSocketAddress hostAddr = (InetSocketAddress) ctx.getRemoteAddress();
        this.senderHost = hostAddr.getAddress().getHostAddress();
        this.hookUrl = hookUrl;
    }

    @Override
    public void from(String from) {
        this.senderEmail = from;
    }

    @Override
    public void recipient(String recipient) {

        this.recipients.add(recipient);
    }

    @Override
    public void data(InputStream is) throws IOException {

        data = IOUtils.toByteArray(is);
    }

    @Override
    public void done() {

        try {

            Session s = Session.getDefaultInstance(new Properties());
            MimeMessage mimeMsg = new MimeMessage(s, new ByteArrayInputStream(data));

            // set any data parse the mimemessage itself
            MessageHeaders headers = getHeaders(mimeMsg);

            Message message = new Message(0,
                    mimeMsg.getMessageID(),
                    DecoderUtil.decodeEncodedWords(headers.get("Subject"), Charsets.UTF_8),
                    this.senderEmail,
                    new Date(),
                    senderHost,
                    this.data.length,
                    IOUtils.toString(data, StandardCharsets.UTF_8.name()),
                    this.recipients,
                    headers
            );

            message.setIsSpam(checkIsSpam(data));
            messageService.saveMessage(message);

            logger.info(String.format("Stored SMTP message '%s' parse %s to: %s",
                    message.getIdentifier(),
                    message.getSenderEmail(),
                    StringUtils.join(message.getRecipients(), ","))
            );

        } catch (Exception e) {

            logger.error("Couldn't handle message: " + e.getMessage(), e);

        }

    }

    private boolean checkIsSpam(byte[] mailData) {
        final RequestBody requestBody = RequestBody.create(MediaType.parse("message/rfc822"), mailData);
        final Request request = new Request.Builder()
                .url(hookUrl)
                .method("POST", requestBody)
                .build();
        try {
            final Response response = okHttpClient.newCall(request).execute();
            String html = new String(response.body().bytes(), StandardCharsets.UTF_8);

            return html.equals("1");
        } catch (IOException | NullPointerException e) {
            logger.error("Failed when check is spam", e);
        }
//        new Callback() {
//
//            @Override
//            public void onFailure(Call call, IOException e) {
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//
//
//            }
//        });
        return false;
    }

    protected MessageHeaders getHeaders(MimeMessage message) throws MessagingException {

        Map<String, String> headerMap = new HashMap<>();

        // oh wow 2015 and it's untyped and uses Enumeration
        Enumeration allHeaders = message.getAllHeaders();
        while (allHeaders.hasMoreElements()) {
            Header header = (Header) allHeaders.nextElement();
            String headerName = header.getName();
            String headerVal = header.getValue();

            headerMap.put(headerName, headerVal);

        }

        return new MessageHeaders(headerMap);
    }

}

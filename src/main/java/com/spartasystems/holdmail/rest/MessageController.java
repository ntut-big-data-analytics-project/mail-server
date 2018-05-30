/*******************************************************************************
 * Copyright 2016 - 2018 Sparta Systems, Inc
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

package com.spartasystems.holdmail.rest;

import com.spartasystems.holdmail.domain.Message;
import com.spartasystems.holdmail.domain.MessageContentPart;
import com.spartasystems.holdmail.mapper.MessageSummaryMapper;
import com.spartasystems.holdmail.model.MessageForwardCommand;
import com.spartasystems.holdmail.model.MessageIsSpamCommand;
import com.spartasystems.holdmail.model.MessageList;
import com.spartasystems.holdmail.model.MessageSummary;
import com.spartasystems.holdmail.persistence.MessageEntity;
import com.spartasystems.holdmail.service.MessageService;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hibernate.validator.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.function.BiConsumer;

import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.http.MediaType.TEXT_PLAIN;

@RestController
@RequestMapping(value = "/rest/messages", produces = MediaType.APPLICATION_JSON_VALUE)
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageSummaryMapper messageSummaryMapper;

    @Autowired
    private HTMLPreprocessor htmlPreprocessor;

    @RequestMapping()
    public MessageList getMessages(
            @RequestParam(name = "recipient", required = false) @Email String recipientEmail,
            @RequestParam(name = "mode", required = false, defaultValue = "-1") Integer mode,
            Pageable pageRequest) {
        return messageService.findMessages(recipientEmail, mode, pageRequest);
    }

    @RequestMapping(value = "/{messageId}", method = RequestMethod.DELETE)
    public ResponseEntity deleteMessage(@PathVariable("messageId") long messageId) {
        messageService.deleteMessage(messageId);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/{messageId}/spam", method = RequestMethod.PUT)
    public ResponseEntity updateMessage(@PathVariable("messageId") long messageId,
                                        @Valid @RequestBody MessageIsSpamCommand spamCommand) {
        messageService.updateMessageSpamFlag(messageId, spamCommand.getIsSpam());
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/{messageId}")
    public ResponseEntity getMessageContent(@PathVariable("messageId") long messageId) {

        MessageSummary summary = loadMessageSummary(messageId);
        return ResponseEntity.ok().body(summary);
    }

    @RequestMapping(value = "/{messageId}/html")
    public ResponseEntity getMessageContentHTML(@PathVariable("messageId") long messageId) {

        MessageSummary summary = loadMessageSummary(messageId);
        String htmlSubstituted = htmlPreprocessor.preprocess(messageId, summary.getMessageBodyHTML());
        return serveContent(htmlSubstituted, TEXT_HTML);
    }

    @RequestMapping(value = "/{messageId}/text")
    public ResponseEntity getMessageContentTEXT(@PathVariable("messageId") long messageId) {

        MessageSummary summary = loadMessageSummary(messageId);
        return serveContent(summary.getMessageBodyText(), TEXT_PLAIN);
    }

    @RequestMapping(value = "/{messageId}/raw")
    public ResponseEntity getMessageContentRAW(@PathVariable("messageId") long messageId) {
        Message message = messageService.getMessage(messageId);
        return serveContent(message.getRawMessage(), TEXT_PLAIN);
    }

    @RequestMapping(value = "/download")
    public ResponseEntity getAllMessages() {
        final List<MessageEntity> list = messageService.listMessages();
        ByteArrayOutputStream fOut = null;
        BufferedOutputStream bOut = null;
        GzipCompressorOutputStream gzOut = null;
        TarArchiveOutputStream tOut = null;
        try {
            fOut = new ByteArrayOutputStream((1 << 20) * 128);
            bOut = new BufferedOutputStream(fOut);
            gzOut = new GzipCompressorOutputStream(bOut);
            tOut = new TarArchiveOutputStream(gzOut);
            final TarArchiveOutputStream finalTOut = tOut;
            final BiConsumer<byte[], String> addFileToTarGz = (content, entryName) -> {
                TarArchiveEntry tarEntry = new TarArchiveEntry(entryName);
                tarEntry.setSize(content.length);
                try {
                    synchronized (finalTOut) {
                        finalTOut.putArchiveEntry(tarEntry);
                        finalTOut.write(content);
                        finalTOut.closeArchiveEntry();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

            list.parallelStream().map(message -> {
                byte[] data = message.getRawMessage().getBytes();
                StringBuilder sb = new StringBuilder();
                DateFormat formatter = new SimpleDateFormat("yyyy-MM");
                sb
                        .append("/")
                        .append(formatter.format(message.getReceivedDate()))
                        .append("/")
                        .append(message.getIdentifier())
                        .append(".eml");

                if (message.getIsSpam()) {
                    sb.insert(0, "spam");
                } else {
                    sb.insert(0, "ham");
                }

                return new ImmutablePair<>(sb.toString(), data);
            }).forEach(pair -> addFileToTarGz.accept(pair.right, pair.left));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (tOut != null) {
                    tOut.finish();
                    tOut.close();
                }
                if (gzOut != null) gzOut.close();
                if (bOut != null) bOut.close();
                if (fOut != null) fOut.close();
            } catch (IOException e) {

            }
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/tar+gzip"))
                .header("Content-Disposition", "attachment;" + " filename=\"messages.tar.gz\";")
                .body(fOut.toByteArray());
    }

    @RequestMapping(value = "/{messageId}/content/{contentId}")
    public ResponseEntity getMessageContentByPartId(@PathVariable("messageId") long messageId,
                                                    @PathVariable("contentId") String contentId) {

        Message message = messageService.getMessage(messageId);

        MessageContentPart content = message.getContent().findByContentId(contentId);

        return ResponseEntity.ok()
                .header("Content-Type", content.getContentType())
                .body(new InputStreamResource(content.getContentStream()));
    }

    @RequestMapping(value = "/{messageId}/att/{attId}")
    public ResponseEntity getMessageContentByAttachmentId(@PathVariable("messageId") long messageId,
                                                          @PathVariable("attId") int attId) {

        Message message = messageService.getMessage(messageId);

        MessageContentPart content = message.getContent().findBySequenceId(attId);

        String disposition = "attachment;";

        if (StringUtils.isNotBlank(content.getAttachmentFilename())) {
            disposition += " filename=\"" + content.getAttachmentFilename() + "\";";
        }

        return ResponseEntity.ok()
                .header("Content-Type", content.getContentType())
                .header("Content-Disposition", disposition)
                .body(new InputStreamResource(content.getContentStream()));
    }

    @RequestMapping(value = "/{messageId}/forward", method = RequestMethod.POST)
    public ResponseEntity fowardMail(@PathVariable("messageId") long messageId,
                                     @Valid @RequestBody MessageForwardCommand forwardCommand) {

        messageService.forwardMessage(messageId, forwardCommand.getRecipient());

        return ResponseEntity.accepted().build();
    }

    // -------------------------- utility ------------------------------------

    protected MessageSummary loadMessageSummary(long messageId) {
        Message message = messageService.getMessage(messageId);
        return messageSummaryMapper.toMessageSummary(message);
    }

    protected ResponseEntity serveContent(Object data, MediaType mediaType) {

        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(mediaType).body(data);
    }

}

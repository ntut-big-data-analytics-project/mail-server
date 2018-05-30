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

package com.spartasystems.holdmail.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Stream;

@Component
public interface MessageRepository extends CrudRepository<MessageEntity, Long> {

    @Query("SELECT m FROM MessageEntity m where (:mode < 0 or m.isSpam = :mode) order by  m.receivedDate desc")
    List<MessageEntity> findAllByOrderByReceivedDateDesc(@Param("mode") Integer mode, Pageable pageable);

    @Query("SELECT m FROM MessageEntity m join m.recipients r where r.recipientEmail = :recipientEmail and (:mode < 0 or m.isSpam = :mode) order by  m.receivedDate desc")
    List<MessageEntity> findAllForRecipientOrderByReceivedDateDesc(
            @Param("mode") Integer mode,
            @Param("recipientEmail") String recipientEmail,
            Pageable pageable
    );

    @Query("SELECT m FROM MessageEntity m order by m.receivedDate desc")
    List<MessageEntity> getAllMessages();

    Stream<MessageEntity> findBySubject(String subject, Pageable pageable);

    Stream<MessageEntity> findBySenderEmail(String senderEmail, Pageable pageable);

    @Query("UPDATE MessageEntity m SET m.isSpam = :val WHERE m.messageId = :id")
    @Modifying
    @Transactional
    void updateSpamValid(@Param("id") long id, @Param("val") boolean val);

}

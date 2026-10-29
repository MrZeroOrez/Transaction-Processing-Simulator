package com.chandramohan.banking.domain;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
@Configuration @EnableScheduling class EventScheduling {}
@Component @ConditionalOnProperty(name="app.kafka.enabled",havingValue="true")
public class EventPipeline {
 private final JdbcTemplate db; private final KafkaTemplate<String,String> kafka; private final TransactionTemplate tx;
 public EventPipeline(JdbcTemplate db,KafkaTemplate<String,String> kafka,PlatformTransactionManager tm){this.db=db;this.kafka=kafka;this.tx=new TransactionTemplate(tm);}
 @Scheduled(fixedDelayString="${app.kafka.poll-ms:5000}") public void publish(){
  var ids=db.queryForList("SELECT id FROM event_outbox WHERE sent=FALSE ORDER BY id LIMIT 20",String.class);
  for(String id:ids)try{tx.executeWithoutResult(status->{
   var rows=db.queryForList("SELECT * FROM event_outbox WHERE id=? AND sent=FALSE FOR UPDATE",id);if(rows.isEmpty())return;
   var row=rows.get(0);String resource=db.queryForObject("SELECT resource_id FROM event_outbox WHERE id=?",String.class,id),type=db.queryForObject("SELECT event_type FROM event_outbox WHERE id=?",String.class,id);
   try{kafka.send("banking.events",resource,id+"|"+resource+"|"+type).get(10,TimeUnit.SECONDS);}catch(Exception e){throw new IllegalStateException("Broker not acknowledged",e);}
   db.update("UPDATE event_outbox SET sent=TRUE WHERE id=?",id);
  });}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(EventPipeline.class).warn("Event retained for retry: {}",id);}
 }
 @KafkaListener(topics="banking.events",groupId="${app.kafka.consumer-group:${spring.application.name}-reporting}") public void consume(String payload){
  String[] parts=payload.split("\\|",-1);if(parts.length!=3)throw new IllegalArgumentException("Invalid event");
  tx.executeWithoutResult(status->{db.update("INSERT INTO processed_events(id,resource_id,event_type) SELECT ?,?,? WHERE NOT EXISTS (SELECT 1 FROM processed_events WHERE id=?)",parts[0],parts[1],parts[2],parts[0]);});
 }
}

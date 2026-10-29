package com.chandramohan.banking;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.chandramohan.banking.domain.EventPipeline;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest
class EventPipelineTest {
 @Autowired JdbcTemplate db; @Autowired PlatformTransactionManager tm;
 @Test void duplicateConsumptionIsIdempotent(){var p=new EventPipeline(db,mock(KafkaTemplate.class),tm);String id=UUID.randomUUID().toString();p.consume(id+"|resource|POSTED");p.consume(id+"|resource|POSTED");assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM processed_events WHERE id=?",Integer.class,id));}
 @Test void acknowledgedPublishMarksSentButFailureRetainsEvent(){KafkaTemplate<String,String> k=mock(KafkaTemplate.class);var p=new EventPipeline(db,k,tm);String id=UUID.randomUUID().toString();db.update("INSERT INTO event_outbox(id,resource_id,event_type,sent) VALUES(?,?,?,FALSE)",id,id,"POSTED");when(k.send(anyString(),anyString(),anyString())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("offline")));p.publish();assertFalse(db.queryForObject("SELECT sent FROM event_outbox WHERE id=?",Boolean.class,id));when(k.send(anyString(),anyString(),anyString())).thenReturn(CompletableFuture.completedFuture(null));p.publish();assertTrue(db.queryForObject("SELECT sent FROM event_outbox WHERE id=?",Boolean.class,id));}
}

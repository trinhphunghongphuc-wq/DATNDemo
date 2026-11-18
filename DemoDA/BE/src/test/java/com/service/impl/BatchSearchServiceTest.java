package com.service.impl;

import com.entity.Batch;
import com.entity.Record;
import com.entity.User;
import com.enums.BatchStatus;
import com.enums.RecordType;
import com.enums.Role;
import com.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:batchsearch;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BatchSearchService.class)
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class,
        TransactionalTestExecutionListener.class}, mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
class BatchSearchServiceTest {
    @Autowired private BatchSearchService search;
    @Autowired private TestEntityManager em;
    private User producer, otherProducer, distributor, otherDistributor, retailer, otherRetailer;
    private final LocalDateTime time = LocalDateTime.of(2026, 11, 12, 10, 0);

    @BeforeEach
    void setup() {
        producer = account("producer", Role.PRODUCER);
        otherProducer = account("producer-other", Role.PRODUCER);
        distributor = account("distributor", Role.DISTRIBUTOR);
        otherDistributor = account("distributor-other", Role.DISTRIBUTOR);
        retailer = account("retailer", Role.RETAILER);
        otherRetailer = account("retailer-other", Role.RETAILER);
    }

    private User account(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("test-password-hash");
        user.setRole(role);
        user.setEnabled(true);
        user.setCreatedAt(time);
        return em.persistAndFlush(user);
    }

    private Batch batch(String code, String name, BatchStatus status, LocalDateTime createdAt,
                        User owner, User assignedDistributor, User assignedRetailer) {
        return em.persistAndFlush(Batch.builder().batchCode(code).name(name).status(status)
                .createdAt(createdAt).createdBy(owner)
                .distributorId(assignedDistributor.getId()).retailerId(assignedRetailer.getId()).build());
    }

    private Batch own(String code, String name, BatchStatus status, LocalDateTime createdAt) {
        return batch(code, name, status, createdAt, producer, distributor, retailer);
    }

    @Test
    void scopesResultsAndTotalsToProducer() {
        Batch own = own("OWN", "Xoài", BatchStatus.CREATED, time);
        batch("SECRET", "Xoài", BatchStatus.CREATED, time, otherProducer, otherDistributor, otherRetailer);
        var result = search.search(new CustomUserDetails(producer), "xoài", "", 0, 10);
        assertEquals(1, result.totalElements());
        assertEquals(own.getId(), result.content().get(0).id());
    }

    @Test
    void scopesResultsToAssignedDistributorAndRetailer() {
        Batch own = own("OWN", "Xoài", BatchStatus.ASSIGNED_TO_DISTRIBUTOR, time);
        batch("OTHER", "Xoài", BatchStatus.ASSIGNED_TO_DISTRIBUTOR, time,
                producer, otherDistributor, otherRetailer);
        for (User user : new User[]{distributor, retailer}) {
            var result = search.search(new CustomUserDetails(user), "", "", 0, 10);
            assertEquals(1, result.totalElements());
            assertEquals(own.getId(), result.content().get(0).id());
        }
    }

    @Test
    void searchesCodeOrNameCaseInsensitivelyAndCombinesStatus() {
        Batch target = own("CODE-ABC", "Xoài Miền Tây", BatchStatus.CREATED, time);
        own("OTHER", "Xoài khác", BatchStatus.IN_DISTRIBUTION, time);
        var user = new CustomUserDetails(producer);
        assertEquals(target.getId(), search.search(user, "code-ab", "", 0, 10).content().get(0).id());
        var filtered = search.search(user, "  XOÀI  ", "created", 0, 10);
        assertEquals(1, filtered.totalElements());
        assertEquals(target.getId(), filtered.content().get(0).id());
    }

    @Test
    void sortsNewestFirstAndUsesIdToBreakTiesAcrossPages() {
        Batch old = own("OLD", "Old", BatchStatus.CREATED, time.minusDays(1));
        Batch first = own("NEW-1", "New", BatchStatus.CREATED, time);
        Batch second = own("NEW-2", "New", BatchStatus.CREATED, time);
        var user = new CustomUserDetails(producer);
        var page0 = search.search(user, "", "", 0, 2);
        assertEquals(3, page0.totalElements());
        assertEquals(2, page0.totalPages());
        assertEquals(second.getId(), page0.content().get(0).id());
        assertEquals(first.getId(), page0.content().get(1).id());
        assertEquals(old.getId(), search.search(user, "", "", 1, 2).content().get(0).id());
    }

    @Test
    void treatsSqlWildcardCharactersLiterally() {
        own("ONE", "100% organic", BatchStatus.CREATED, time);
        own("TWO", "1000 organic", BatchStatus.CREATED, time);
        own("THREE", "A_organic", BatchStatus.CREATED, time);
        own("FOUR", "Aorganic", BatchStatus.CREATED, time);
        own("FIVE", "Hello!", BatchStatus.CREATED, time);
        var user = new CustomUserDetails(producer);
        assertEquals(1, search.search(user, "%", "", 0, 10).totalElements());
        assertEquals(1, search.search(user, "_", "", 0, 10).totalElements());
        assertEquals(1, search.search(user, "!", "", 0, 10).totalElements());
    }

    @Test
    void returnsRecordCountsWithoutLoadingTheWholeRecordCollection() {
        Batch batch = own("COUNT", "Record count", BatchStatus.CREATED, time);
        for (int i = 0; i < 2; i++) {
            em.persist(Record.builder().batch(batch).recordKey("RECORD-" + i)
                    .recordType(RecordType.PRODUCTION).rawJson("{}").build());
        }
        em.flush();
        em.clear();
        var result = search.search(new CustomUserDetails(producer), "", "", 0, 10);
        assertEquals(2, result.content().get(0).recordCount());
    }

    @Test
    void rejectsInvalidPageSizeAndStatus() {
        var user = new CustomUserDetails(producer);
        assertThrows(IllegalArgumentException.class, () -> search.search(user, "", "", -1, 10));
        assertThrows(IllegalArgumentException.class, () -> search.search(user, "", "", 0, 1000));
        assertThrows(IllegalArgumentException.class, () -> search.search(user, "", "BAD_STATUS", 0, 10));
    }

    @Test
    void rejectsDisabledAccountsAndUnsupportedRoles() {
        producer.setEnabled(false);
        assertThrows(AccessDeniedException.class,
                () -> search.search(new CustomUserDetails(producer), "", "", 0, 10));
        var consumer = account("consumer", Role.CONSUMER);
        assertThrows(AccessDeniedException.class,
                () -> search.search(new CustomUserDetails(consumer), "", "", 0, 10));
    }
}

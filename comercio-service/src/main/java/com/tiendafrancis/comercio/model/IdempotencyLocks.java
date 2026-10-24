package com.tiendafrancis.comercio.model;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IdempotencyLocks {

 private final EntityManager entityManager;

 @Transactional(propagation = Propagation.MANDATORY)
 public void acquire(String namespace, UUID key) {

  if (key == null) {
   throw new IllegalArgumentException(
           "Idempotency key required"
   );
  }

  entityManager.createNativeQuery(
                  "SELECT pg_advisory_xact_lock(" +
                          "hashtextextended(:lock_key, 0))"
          )
          .setParameter("lock_key", namespace + ":" + key)
          .getSingleResult();
 }
}

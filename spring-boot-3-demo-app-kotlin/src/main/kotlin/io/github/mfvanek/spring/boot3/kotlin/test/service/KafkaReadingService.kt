/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.kotlin.test.service

import io.github.mfvanek.db.migrations.common.saver.DbSaver
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.dao.DataAccessException
import org.springframework.kafka.KafkaException
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.*

private val logger = KotlinLogging.logger {}

internal object KafkaHeadersGetter : Propagator.Getter<ConsumerRecord<UUID, String>> {
    override fun get(carrier: ConsumerRecord<UUID, String>, key: String): String? =
        carrier.headers()?.lastHeader(key)?.value()?.toString(StandardCharsets.UTF_8)
}

@Service
class KafkaReadingService(
    private val tracer: Tracer,
    private val propagator: Propagator,
    private val dbSaver: DbSaver
) {
    @KafkaListener(topics = ["\${spring.kafka.template.default-topic}"])
    fun listen(record: ConsumerRecord<UUID, String>, ack: Acknowledgment) {
        dbSaver.processSingleRecord(record)
        ack.acknowledge()
    }

    @KafkaListener(
        id = "\${spring.kafka.consumer.additional-groupId}",
        topics = ["\${spring.kafka.template.additional-topic}"],
        batch = "true"
    )
    fun listenAdditional(records: List<ConsumerRecord<UUID, String>>, ack: Acknowledgment) {
        val batchSpan = tracer.startScopedSpan("batch-processing")
        try {
            logger.info {
                "Received from Kafka ${records.size} records"
            }
            records.forEach { record -> restoreContextAndProcessSingleRecordIfNeed(record) }
            ack.acknowledge()
        } catch (e: KafkaException) {
            batchSpan.error(e)
            throw e
        } finally {
            batchSpan.end()
        }
    }

    private fun restoreContextAndProcessSingleRecordIfNeed(record: ConsumerRecord<UUID, String>) {
        val builder = propagator.extract(record, KafkaHeadersGetter)
        val spanFromRecord = builder.name("processing-record-from-kafka").start()
        try {
            tracer.withSpan(spanFromRecord).use {
                dbSaver.processSingleRecord(record)
            }
        } catch (e: DataAccessException) {
            spanFromRecord.error(e)
            throw e
        } finally {
            spanFromRecord.end()
        }
    }
}

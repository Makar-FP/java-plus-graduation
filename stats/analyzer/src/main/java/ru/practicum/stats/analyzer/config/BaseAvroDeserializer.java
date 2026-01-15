package ru.practicum.stats.analyzer.config;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Objects;

public class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {
    private final DecoderFactory decoderFactory;
    protected final Schema schema;
    private final DatumReader<T> reader;
    private BinaryDecoder decoder;

    public BaseAvroDeserializer(Schema schema) {
        this(DecoderFactory.get(), schema);
    }

    public BaseAvroDeserializer(DecoderFactory decoderFactory, Schema schema) {
        this.decoderFactory = Objects.requireNonNull(decoderFactory, "decoderFactory");
        this.schema = Objects.requireNonNull(schema, "schema");
        this.reader = new SpecificDatumReader<>(this.schema);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        try {
            if (data == null || data.length == 0) {
                return null;
            }

            decoder = decoderFactory.binaryDecoder(data, decoder);
            return reader.read(null, decoder);
        } catch (Exception e) {
            throw new SerializationException("Error with data desirialization [" + topic + "]", e);
        }
    }
}

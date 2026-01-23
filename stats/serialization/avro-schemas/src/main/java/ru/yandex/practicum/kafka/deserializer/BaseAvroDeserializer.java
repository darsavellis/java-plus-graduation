package ru.yandex.practicum.kafka.deserializer;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

public abstract class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {
    final DecoderFactory decoderFactory;
    final SpecificDatumReader<T> specificDatumReader;

    public BaseAvroDeserializer(Schema schema) {
        this(DecoderFactory.get(), schema);
    }

    public BaseAvroDeserializer(DecoderFactory decoderFactory, Schema schema) {
        this.decoderFactory = decoderFactory;
        this.specificDatumReader = new SpecificDatumReader<>(schema);
    }

    @Override
    public T deserialize(String topic, byte[] bytes) {
        BinaryDecoder binaryDecoder = decoderFactory.binaryDecoder(bytes, null);
        try {
            return specificDatumReader.read(null, binaryDecoder);
        } catch (Exception ex) {
            throw new SerializationException("Error deserializing data from topic + [" + topic + "]");
        }
    }
}

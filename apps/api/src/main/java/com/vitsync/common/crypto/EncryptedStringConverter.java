package com.vitsync.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Conversor JPA que cifra un {@code String} al guardarlo y lo descifra al leerlo.
 *
 * <p>Se aplica campo a campo, nunca de forma global:
 * <pre>{@code
 * @Convert(converter = EncryptedStringConverter.class)
 * @Column(name = "nif_encrypted")
 * private String nif;
 * }</pre>
 * La entidad trabaja con el valor en claro y la columna {@code *_encrypted} recibe el cifrado.
 *
 * <p>Inyección por constructor: Spring Boot configura Hibernate para pedirle los conversores
 * a Spring ({@code SpringBeanContainer}), que los crea resolviendo sus dependencias. Por eso
 * no lleva {@code @Component} y aun así recibe el {@link FieldEncryptor}. Así no hace falta
 * un campo estático "rellenado a mano", que es el apaño típico y rompe los tests.
 *
 * <p>{@code null} se guarda como {@code NULL}: un campo opcional vacío no se cifra.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final FieldEncryptor encryptor;

    public EncryptedStringConverter(FieldEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : encryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : encryptor.decrypt(dbData);
    }
}

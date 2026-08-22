package com.algaworks.algashop.product.catalog.infrastructure.storage.s3;

import com.algaworks.algashop.product.catalog.application.storage.FileReference;
import com.algaworks.algashop.product.catalog.application.storage.StorageProvider;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Exception;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URL;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StorageProviderAwsS3Impl implements StorageProvider {

    private final StorageProviderAwsS3properties properties;
    private final S3Template s3Template;

    @Override
    public boolean healthCheck() {
        try {
            return s3Template.bucketExists(properties.getBucketName());
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    @SneakyThrows
    public URL requestUploadUrl(FileReference fileReference) {
        String bucketName = properties.getBucketName();
        String key = fileReference.getFileName();
        if (fileExists(key)) {
            throw new StorageProviderException(String.format("Remote file %s already exists", key));
        }

        ObjectMetadata.Builder metadata = ObjectMetadata.builder()
                .acl("public-read");

        if (fileReference.isAllowPublicRead()) {
            metadata.acl("public-read");
        }

        try {
           return s3Template.createSignedPutURL(
                    bucketName,
                    key,
                    fileReference.getExpiresIn(),
                    metadata.build(),
                    fileReference.getContentType().toString()
            );
        }catch (S3Exception ex) {
            throw new StorageProviderException(String.format("Unknow error when tried to create presigned URL for File %s", key), ex);
        }
    }

    @Override
    public void deleteFile(String remoteFileName) {
        if (!fileExists(remoteFileName)) {
            throw new StorageProviderException(String.format("Remote file %s does not exists", remoteFileName));
        }
        try {
            s3Template.deleteObject(properties.getBucketName(), remoteFileName);
        } catch (S3Exception ex) {
            throw new StorageProviderException(String.format("Unknow error when tried to delete File %s", remoteFileName), ex);
        }
    }

    @Override
    public boolean fileExists(String remoteFileName) {
        return s3Template.objectExists(properties.getBucketName(), remoteFileName);
    }
}

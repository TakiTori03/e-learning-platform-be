package com.hust.workerservice.strategy;

import java.io.IOException;

public interface StorageStrategy {

    void uploadDirectory(String localPath, String remotePath) throws IOException;
    void deleteFile(String fileUrl) throws IOException;
    void downloadFileToLocal(String path, java.io.File destinationFile) throws IOException;
    void uploadFile(String localFilePath, String remoteKey) throws IOException;
}


package com.github.mcollovati.vertxvaadin.flowdemo.ui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.flow.component.upload.Receiver;
import com.vaadin.flow.function.SerializableFunction;

public interface SerializableUtils<T> extends Comparator<T>, Serializable {

    static <T, U> Function<T, U> function(SerializableFunction<T, U> fn) {
        return fn;
    }

    static <T> ToIntFunction<T> toIntFunction(SerializableToIntFunction<T> fn) {
        return fn;
    }

    interface SerializableToIntFunction<R> extends ToIntFunction<R>, Serializable {}

    interface FileReceiver extends Receiver {
        FileInfo getFileInfo();

        default String getFileName() {
            return Optional.ofNullable(getFileInfo()).map(FileInfo::getFileName).orElse("");
        }

        default InputStream getInputStream() {
            FileInfo file = getFileInfo();
            if (file != null) {
                try {
                    return new FileInputStream(file.file);
                } catch (IOException e) {
                    Logger.getLogger(getClass().getName()).log(Level.WARNING,
                        "Failed to create InputStream for: '" + getFileName()
                            + "'", e);
                }
            }
            return new ByteArrayInputStream(new byte[0]);
        }

        class FileInfo implements Serializable {
            private final File file;
            private final String fileName;
            private final String mimeType;

            public FileInfo(File file, String fileName, String mimeType) {
                this.file = file;
                this.fileName = fileName;
                this.mimeType = mimeType;
            }

            public String getFileName() {
                return fileName;
            }

            OutputStream getOutputBuffer() {
                try {
                    return new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    static FileReceiver newFileBuffer() {
        return new SerializableFileBuffer();
    }
}

class SerializableFileBuffer implements SerializableUtils.FileReceiver {

    private FileInfo fileInfo;

    @Override
    public OutputStream receiveUpload(String fileName, String mimeType) {
        try {
            final String tempFileName = "upload_tmpfile_" + fileName + "_"
                + System.currentTimeMillis();
            File file = File.createTempFile(tempFileName, null);
            fileInfo = new FileInfo(file, fileName, mimeType);
            return new FileOutputStream(file);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE,
                "Failed to create file output stream for: '" + fileName
                    + "'",
                e);
        }
        return null;
    }


    public FileInfo getFileInfo() {
        return fileInfo;
    }

    protected Logger getLogger() {
        return Logger.getLogger(this.getClass().getName());
    }
}

class SerializableFileOutputStream extends FileOutputStream implements Serializable {

    private final File file;

    public SerializableFileOutputStream(File file) throws FileNotFoundException {
        super(file);
        this.file = file;
    }

    private Object writeReplace() throws ObjectStreamException {
        return new FilePathHolder(file);
    }


    private static class FilePathHolder implements Serializable {
        private final File path;

        public FilePathHolder(File path) {
            this.path = path;
        }

        private Object readResolve() throws ObjectStreamException {
            try {
                return new FileOutputStream(path);
            } catch (FileNotFoundException e) {
                throw new InvalidObjectException(e.getMessage());
            }
        }
    }

}

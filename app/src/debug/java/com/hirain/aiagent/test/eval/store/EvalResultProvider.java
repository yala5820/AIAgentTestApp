package com.hirain.aiagent.test.eval.store;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * 已获批准的 Debug-only 结果读取通道，解决车机 secondary user 无法 adb run-as
 * 且 shell 无权读取 app-specific 外部目录的问题。它严格只读、只接受安全 correlationId，
 * Release manifest 不包含该 Provider。
 */
public final class EvalResultProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("read-only");
        if (uri.getPathSegments().size() != 1) throw new FileNotFoundException("invalid path");
        String correlationId = uri.getLastPathSegment();
        if (correlationId == null || !correlationId.matches("^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$")) throw new FileNotFoundException("invalid correlationId");
        File root = new File(getContext().getExternalFilesDir(null), "eval-results");
        File target = new File(root, correlationId + ".json");
        if (!target.isFile()) throw new FileNotFoundException(correlationId);
        return ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public String getType(Uri uri) { return "application/json"; }
    @Override public Cursor query(Uri uri, String[] p, String s, String[] a, String o) { return null; }
    @Override public android.os.Bundle call(String method, String arg, android.os.Bundle extras) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { throw new UnsupportedOperationException("read-only"); }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException("read-only"); }
    @Override public Uri insert(Uri uri, ContentValues v) { throw new UnsupportedOperationException("read-only"); }
}

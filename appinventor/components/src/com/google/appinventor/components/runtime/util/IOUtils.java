// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2016-2017 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;

import android.util.Log;
import java.io.InputStream;

public final class IOUtils {
  /**
   * Closes the given {@code Closeable}. Suppresses any IO exceptions.
   */
  public static void closeQuietly(String tag, Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (IOException e) {
        Log.w(tag, "Failed to close resource", e);
    }
  }

  /**
   * Reads the contents of a stream into a String, using the default charset
   * (UTF-8 on Android).
   *
   * @param stream the stream of data to read
   * @return the stream contents interpreted as UTF-8
   * @throws IOException if the stream cannot be read
   */
  public static String readStream(InputStream stream) throws IOException {
    byte[] chunk = new byte[4096];
    int read;
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    while ((read = stream.read(chunk)) > 0) {
      buffer.write(chunk, 0, read);
    }
    return buffer.toString();
  }
}

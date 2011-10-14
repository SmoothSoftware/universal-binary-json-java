/**   
 * Copyright 2011 The Buzz Media, LLC
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ubjson.io.charset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class StreamDecoder {
	public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	
	private static final char[] EMPTY = new char[0];

	private byte[] readBuffer;
	private char[] decodeBuffer;

	private CharsetDecoder decoder;

	public StreamDecoder() {
		this(UTF8_CHARSET);
	}

	public StreamDecoder(Charset charset) throws IllegalArgumentException {
		if (charset == null)
			throw new IllegalArgumentException("charset cannot be null");

		readBuffer = new byte[8192];
		decodeBuffer = new char[8192];
		decoder = charset.newDecoder();
	}

	public char[] decode(InputStream stream, int length)
			throws IllegalArgumentException, IOException {
		if (stream == null)
			throw new IllegalArgumentException("stream cannot be null");
		if (length < 0)
			throw new IllegalArgumentException("length [" + length
					+ "] must be >= 0.");
		
		// short-circuit
		if(length == 0)
			return EMPTY;

		int charCount = 0;
		char[] chars = new char[length];

		// Reuse the backing decode buffer.
		CharBuffer dest = CharBuffer.wrap(decodeBuffer);

		int bytesRead = 0;
		decoder.reset();

		while (length > 0
				&& (bytesRead = stream.read(readBuffer, 0, length)) != -1) {
			length -= bytesRead;
			boolean done = (length == 0);

			ByteBuffer src = ByteBuffer.wrap(readBuffer, 0, bytesRead);
			dest.clear();
			decoder.decode(src, dest, done);
			dest.flip();

			int remaining = dest.remaining();
			dest.get(chars, charCount, remaining);
			charCount += remaining;
		}

		if (length > 0)
			throw new IOException(
					"End of Stream encountered before all requested bytes ["
							+ (length + bytesRead)
							+ "] could be read. Unable to read the last "
							+ length + " remaining bytes.");

		dest.clear();
		decoder.flush(dest);
		dest.flip();

		if (dest.hasRemaining()) {
			int remaining = dest.remaining();
			dest.get(chars, charCount, remaining);
			charCount += remaining;
		}

		if (charCount < chars.length) {
			char[] tmp = new char[charCount];
			System.arraycopy(chars, 0, tmp, 0, charCount);
			chars = tmp;
		}

		return chars;
	}
}
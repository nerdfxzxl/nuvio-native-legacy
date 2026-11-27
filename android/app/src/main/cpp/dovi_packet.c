#include <jni.h>
#include "dts/dv7.h"

/* Both direct buffers belong to one codec adapter. Transform into separate
 * storage so a larger RPU cannot overwrite a NAL still waiting to be read. */
JNIEXPORT jint JNICALL Java_space_nuvio_nativelegacy_DvPacket_convert(
    JNIEnv *env, jobject self, jobject input, jint offset, jint length, jobject scratch) {
    (void)self;
    uint8_t *src = (*env)->GetDirectBufferAddress(env, input);
    uint8_t *out = (*env)->GetDirectBufferAddress(env, scratch);
    jlong capacity = (*env)->GetDirectBufferCapacity(env, input);
    jlong space = (*env)->GetDirectBufferCapacity(env, scratch);
    if (!src || !out || offset < 0 || length <= 0 || (jlong)offset + length > capacity || space <= 0)
        return -1;
    if (space > capacity - offset) space = capacity - offset;
    size_t written = 0;
    if (nv_dv7_packet(src + offset, (size_t)length, out, (size_t)space, &written) ||
        written > INT_MAX) return -1;
    memcpy(src + offset, out, written);
    return (jint)written;
}

//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: proto/chat.proto

package com.mwojnar.gen;

@kotlin.jvm.JvmSynthetic
public inline fun message(block: com.mwojnar.gen.MessageKt.Dsl.() -> kotlin.Unit): com.mwojnar.gen.Message =
  com.mwojnar.gen.MessageKt.Dsl._create(com.mwojnar.gen.Message.newBuilder()).apply { block() }._build()
public object MessageKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.mwojnar.gen.Message.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.mwojnar.gen.Message.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.mwojnar.gen.Message = _builder.build()

    /**
     * <code>optional uint64 id = 1;</code>
     */
    public var id: kotlin.Long
      @JvmName("getId")
      get() = _builder.getId()
      @JvmName("setId")
      set(value) {
        _builder.setId(value)
      }
    /**
     * <code>optional uint64 id = 1;</code>
     */
    public fun clearId() {
      _builder.clearId()
    }
    /**
     * <code>optional uint64 id = 1;</code>
     * @return Whether the id field is set.
     */
    public fun hasId(): kotlin.Boolean {
      return _builder.hasId()
    }

    /**
     * <code>optional uint64 replyId = 2;</code>
     */
    public var replyId: kotlin.Long
      @JvmName("getReplyId")
      get() = _builder.getReplyId()
      @JvmName("setReplyId")
      set(value) {
        _builder.setReplyId(value)
      }
    /**
     * <code>optional uint64 replyId = 2;</code>
     */
    public fun clearReplyId() {
      _builder.clearReplyId()
    }
    /**
     * <code>optional uint64 replyId = 2;</code>
     * @return Whether the replyId field is set.
     */
    public fun hasReplyId(): kotlin.Boolean {
      return _builder.hasReplyId()
    }

    /**
     * <code>optional uint64 ackId = 3;</code>
     */
    public var ackId: kotlin.Long
      @JvmName("getAckId")
      get() = _builder.getAckId()
      @JvmName("setAckId")
      set(value) {
        _builder.setAckId(value)
      }
    /**
     * <code>optional uint64 ackId = 3;</code>
     */
    public fun clearAckId() {
      _builder.clearAckId()
    }
    /**
     * <code>optional uint64 ackId = 3;</code>
     * @return Whether the ackId field is set.
     */
    public fun hasAckId(): kotlin.Boolean {
      return _builder.hasAckId()
    }

    /**
     * <code>optional uint64 userId = 4;</code>
     */
    public var userId: kotlin.Long
      @JvmName("getUserId")
      get() = _builder.getUserId()
      @JvmName("setUserId")
      set(value) {
        _builder.setUserId(value)
      }
    /**
     * <code>optional uint64 userId = 4;</code>
     */
    public fun clearUserId() {
      _builder.clearUserId()
    }
    /**
     * <code>optional uint64 userId = 4;</code>
     * @return Whether the userId field is set.
     */
    public fun hasUserId(): kotlin.Boolean {
      return _builder.hasUserId()
    }

    /**
     * <code>.chat.Priority priority = 5;</code>
     */
    public var priority: com.mwojnar.gen.Priority
      @JvmName("getPriority")
      get() = _builder.getPriority()
      @JvmName("setPriority")
      set(value) {
        _builder.setPriority(value)
      }
    /**
     * <code>.chat.Priority priority = 5;</code>
     */
    public fun clearPriority() {
      _builder.clearPriority()
    }

    /**
     * <code>string text = 6;</code>
     */
    public var text: kotlin.String
      @JvmName("getText")
      get() = _builder.getText()
      @JvmName("setText")
      set(value) {
        _builder.setText(value)
      }
    /**
     * <code>string text = 6;</code>
     */
    public fun clearText() {
      _builder.clearText()
    }

    /**
     * <code>optional uint64 time = 7;</code>
     */
    public var time: kotlin.Long
      @JvmName("getTime")
      get() = _builder.getTime()
      @JvmName("setTime")
      set(value) {
        _builder.setTime(value)
      }
    /**
     * <code>optional uint64 time = 7;</code>
     */
    public fun clearTime() {
      _builder.clearTime()
    }
    /**
     * <code>optional uint64 time = 7;</code>
     * @return Whether the time field is set.
     */
    public fun hasTime(): kotlin.Boolean {
      return _builder.hasTime()
    }

    /**
     * <code>optional bytes media = 8;</code>
     */
    public var media: com.google.protobuf.ByteString
      @JvmName("getMedia")
      get() = _builder.getMedia()
      @JvmName("setMedia")
      set(value) {
        _builder.setMedia(value)
      }
    /**
     * <code>optional bytes media = 8;</code>
     */
    public fun clearMedia() {
      _builder.clearMedia()
    }
    /**
     * <code>optional bytes media = 8;</code>
     * @return Whether the media field is set.
     */
    public fun hasMedia(): kotlin.Boolean {
      return _builder.hasMedia()
    }

    /**
     * <code>optional string mime = 9;</code>
     */
    public var mime: kotlin.String
      @JvmName("getMime")
      get() = _builder.getMime()
      @JvmName("setMime")
      set(value) {
        _builder.setMime(value)
      }
    /**
     * <code>optional string mime = 9;</code>
     */
    public fun clearMime() {
      _builder.clearMime()
    }
    /**
     * <code>optional string mime = 9;</code>
     * @return Whether the mime field is set.
     */
    public fun hasMime(): kotlin.Boolean {
      return _builder.hasMime()
    }
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.mwojnar.gen.Message.copy(block: com.mwojnar.gen.MessageKt.Dsl.() -> kotlin.Unit): com.mwojnar.gen.Message =
  com.mwojnar.gen.MessageKt.Dsl._create(this.toBuilder()).apply { block() }._build()

// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: proto/chat.proto

package com.mwojnar.gen;

public final class ChatProto {
  private ChatProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_chat_Message_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_chat_Message_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_chat_UserInfo_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_chat_UserInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_chat_Empty_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_chat_Empty_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\020proto/chat.proto\022\004chat\"\371\001\n\007Message\022\017\n\002" +
      "id\030\001 \001(\004H\000\210\001\001\022\024\n\007replyId\030\002 \001(\004H\001\210\001\001\022\022\n\005a" +
      "ckId\030\003 \001(\004H\002\210\001\001\022\023\n\006userId\030\004 \001(\004H\003\210\001\001\022 \n\010" +
      "priority\030\005 \001(\0162\016.chat.Priority\022\014\n\004text\030\006" +
      " \001(\t\022\014\n\004time\030\007 \001(\004\022\022\n\005media\030\010 \001(\014H\004\210\001\001\022\021" +
      "\n\004mime\030\t \001(\tH\005\210\001\001B\005\n\003_idB\n\n\010_replyIdB\010\n\006" +
      "_ackIdB\t\n\007_userIdB\010\n\006_mediaB\007\n\005_mime\"j\n\010" +
      "UserInfo\022\023\n\006userId\030\001 \001(\004H\000\210\001\001\022\024\n\007groupId" +
      "\030\002 \001(\004H\001\210\001\001\022\022\n\005ackId\030\003 \001(\004H\002\210\001\001B\t\n\007_user" +
      "IdB\n\n\010_groupIdB\010\n\006_ackId\"\007\n\005Empty*5\n\010Pri" +
      "ority\022\010\n\004HIGH\020\000\022\n\n\006MEDIUM\020\001\022\n\n\006NORMAL\020\002\022" +
      "\007\n\003LOW\020\0032`\n\004Chat\022-\n\013SendMessage\022\r.chat.M" +
      "essage\032\013.chat.Empty\"\000(\001\022)\n\004Join\022\016.chat.U" +
      "serInfo\032\r.chat.Message\"\0000\001B\036\n\017com.mwojna" +
      "r.genB\tChatProtoP\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_chat_Message_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_chat_Message_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_chat_Message_descriptor,
        new java.lang.String[] { "Id", "ReplyId", "AckId", "UserId", "Priority", "Text", "Time", "Media", "Mime", "Id", "ReplyId", "AckId", "UserId", "Media", "Mime", });
    internal_static_chat_UserInfo_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_chat_UserInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_chat_UserInfo_descriptor,
        new java.lang.String[] { "UserId", "GroupId", "AckId", "UserId", "GroupId", "AckId", });
    internal_static_chat_Empty_descriptor =
      getDescriptor().getMessageTypes().get(2);
    internal_static_chat_Empty_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_chat_Empty_descriptor,
        new java.lang.String[] { });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: D:\code\android\forSdk\Sdk\build-tools\35.0.0\aidl.exe -pD:\code\android\forSdk\Sdk\platforms\android-35\framework.aidl -oD:\code\android\AndroidStudioProjects\AIAgent\app\build\generated\aidl_source_output_dir\debug\out -ID:\code\android\AndroidStudioProjects\AIAgent\app\src\main\aidl -ID:\code\android\AndroidStudioProjects\AIAgent\app\src\debug\aidl -IC:\Users\yala5\.gradle\caches\8.11.1\transforms\7dee0c696b5f4f8c3ab765b971151a0f\transformed\core-1.13.0\aidl -IC:\Users\yala5\.gradle\caches\8.11.1\transforms\5ccdbc8ad7d7d7dc36f9eb6d515eaabe\transformed\versionedparcelable-1.1.1\aidl -dC:\Users\yala5\AppData\Local\Temp\aidl16093990705043187462.d D:\code\android\AndroidStudioProjects\AIAgent\app\src\main\aidl\com\hirain\aiagent\IAIAgentAidlInterface.aidl
 */
package com.hirain.aiagent;
public interface IAIAgentAidlInterface extends android.os.IInterface
{
  /** Default implementation for IAIAgentAidlInterface. */
  public static class Default implements com.hirain.aiagent.IAIAgentAidlInterface
  {
    @Override public void processAgentRequest(com.hirain.aiagent.AgentRequest request) throws android.os.RemoteException
    {
    }
    @Override public com.hirain.aiagent.ConversationOperationResult createConversation(com.hirain.aiagent.ConversationRequest request) throws android.os.RemoteException
    {
      return null;
    }
    @Override public com.hirain.aiagent.ConversationListResponse listConversations(java.lang.String userId) throws android.os.RemoteException
    {
      return null;
    }
    @Override public com.hirain.aiagent.ConversationOperationResult deleteConversation(java.lang.String userId, java.lang.String sessionId) throws android.os.RemoteException
    {
      return null;
    }
    @Override public com.hirain.aiagent.ConversationOperationResult switchConversation(java.lang.String userId, java.lang.String sessionId) throws android.os.RemoteException
    {
      return null;
    }
    @Override public com.hirain.aiagent.ConversationInfo getActiveConversation(java.lang.String userId) throws android.os.RemoteException
    {
      return null;
    }
    @Override public com.hirain.aiagent.CancelRequestResult cancelAgentRequest(java.lang.String requestId, java.lang.String reason) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void registerListener(com.hirain.aiagent.IAIAgentAidlListener listener) throws android.os.RemoteException
    {
    }
    @Override public void unregisterListener(com.hirain.aiagent.IAIAgentAidlListener listener) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.hirain.aiagent.IAIAgentAidlInterface
  {
    /** Construct the stub at attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.hirain.aiagent.IAIAgentAidlInterface interface,
     * generating a proxy if needed.
     */
    public static com.hirain.aiagent.IAIAgentAidlInterface asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.hirain.aiagent.IAIAgentAidlInterface))) {
        return ((com.hirain.aiagent.IAIAgentAidlInterface)iin);
      }
      return new com.hirain.aiagent.IAIAgentAidlInterface.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
      }
      switch (code)
      {
        case TRANSACTION_processAgentRequest:
        {
          com.hirain.aiagent.AgentRequest _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.hirain.aiagent.AgentRequest.CREATOR);
          this.processAgentRequest(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_createConversation:
        {
          com.hirain.aiagent.ConversationRequest _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.hirain.aiagent.ConversationRequest.CREATOR);
          com.hirain.aiagent.ConversationOperationResult _result = this.createConversation(_arg0);
          reply.writeNoException();
          _Parcel.writeTypedObject(reply, _result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_listConversations:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          com.hirain.aiagent.ConversationListResponse _result = this.listConversations(_arg0);
          reply.writeNoException();
          _Parcel.writeTypedObject(reply, _result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_deleteConversation:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          com.hirain.aiagent.ConversationOperationResult _result = this.deleteConversation(_arg0, _arg1);
          reply.writeNoException();
          _Parcel.writeTypedObject(reply, _result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_switchConversation:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          com.hirain.aiagent.ConversationOperationResult _result = this.switchConversation(_arg0, _arg1);
          reply.writeNoException();
          _Parcel.writeTypedObject(reply, _result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getActiveConversation:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          com.hirain.aiagent.ConversationInfo _result = this.getActiveConversation(_arg0);
          reply.writeNoException();
          _Parcel.writeTypedObject(reply, _result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_cancelAgentRequest:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          com.hirain.aiagent.CancelRequestResult _result = this.cancelAgentRequest(_arg0, _arg1);
          reply.writeNoException();
          _Parcel.writeTypedObject(reply, _result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_registerListener:
        {
          com.hirain.aiagent.IAIAgentAidlListener _arg0;
          _arg0 = com.hirain.aiagent.IAIAgentAidlListener.Stub.asInterface(data.readStrongBinder());
          this.registerListener(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_unregisterListener:
        {
          com.hirain.aiagent.IAIAgentAidlListener _arg0;
          _arg0 = com.hirain.aiagent.IAIAgentAidlListener.Stub.asInterface(data.readStrongBinder());
          this.unregisterListener(_arg0);
          reply.writeNoException();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.hirain.aiagent.IAIAgentAidlInterface
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public void processAgentRequest(com.hirain.aiagent.AgentRequest request) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, request, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_processAgentRequest, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public com.hirain.aiagent.ConversationOperationResult createConversation(com.hirain.aiagent.ConversationRequest request) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        com.hirain.aiagent.ConversationOperationResult _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, request, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_createConversation, _data, _reply, 0);
          _reply.readException();
          _result = _Parcel.readTypedObject(_reply, com.hirain.aiagent.ConversationOperationResult.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public com.hirain.aiagent.ConversationListResponse listConversations(java.lang.String userId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        com.hirain.aiagent.ConversationListResponse _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(userId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_listConversations, _data, _reply, 0);
          _reply.readException();
          _result = _Parcel.readTypedObject(_reply, com.hirain.aiagent.ConversationListResponse.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public com.hirain.aiagent.ConversationOperationResult deleteConversation(java.lang.String userId, java.lang.String sessionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        com.hirain.aiagent.ConversationOperationResult _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(userId);
          _data.writeString(sessionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_deleteConversation, _data, _reply, 0);
          _reply.readException();
          _result = _Parcel.readTypedObject(_reply, com.hirain.aiagent.ConversationOperationResult.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public com.hirain.aiagent.ConversationOperationResult switchConversation(java.lang.String userId, java.lang.String sessionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        com.hirain.aiagent.ConversationOperationResult _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(userId);
          _data.writeString(sessionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_switchConversation, _data, _reply, 0);
          _reply.readException();
          _result = _Parcel.readTypedObject(_reply, com.hirain.aiagent.ConversationOperationResult.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public com.hirain.aiagent.ConversationInfo getActiveConversation(java.lang.String userId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        com.hirain.aiagent.ConversationInfo _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(userId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getActiveConversation, _data, _reply, 0);
          _reply.readException();
          _result = _Parcel.readTypedObject(_reply, com.hirain.aiagent.ConversationInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public com.hirain.aiagent.CancelRequestResult cancelAgentRequest(java.lang.String requestId, java.lang.String reason) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        com.hirain.aiagent.CancelRequestResult _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(requestId);
          _data.writeString(reason);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cancelAgentRequest, _data, _reply, 0);
          _reply.readException();
          _result = _Parcel.readTypedObject(_reply, com.hirain.aiagent.CancelRequestResult.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void registerListener(com.hirain.aiagent.IAIAgentAidlListener listener) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(listener);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerListener, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void unregisterListener(com.hirain.aiagent.IAIAgentAidlListener listener) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(listener);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterListener, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_processAgentRequest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_createConversation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_listConversations = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_deleteConversation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_switchConversation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getActiveConversation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_cancelAgentRequest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_registerListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_unregisterListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "com.hirain.aiagent.IAIAgentAidlInterface";
  public void processAgentRequest(com.hirain.aiagent.AgentRequest request) throws android.os.RemoteException;
  public com.hirain.aiagent.ConversationOperationResult createConversation(com.hirain.aiagent.ConversationRequest request) throws android.os.RemoteException;
  public com.hirain.aiagent.ConversationListResponse listConversations(java.lang.String userId) throws android.os.RemoteException;
  public com.hirain.aiagent.ConversationOperationResult deleteConversation(java.lang.String userId, java.lang.String sessionId) throws android.os.RemoteException;
  public com.hirain.aiagent.ConversationOperationResult switchConversation(java.lang.String userId, java.lang.String sessionId) throws android.os.RemoteException;
  public com.hirain.aiagent.ConversationInfo getActiveConversation(java.lang.String userId) throws android.os.RemoteException;
  public com.hirain.aiagent.CancelRequestResult cancelAgentRequest(java.lang.String requestId, java.lang.String reason) throws android.os.RemoteException;
  public void registerListener(com.hirain.aiagent.IAIAgentAidlListener listener) throws android.os.RemoteException;
  public void unregisterListener(com.hirain.aiagent.IAIAgentAidlListener listener) throws android.os.RemoteException;
  /** @hide */
  static class _Parcel {
    static private <T> T readTypedObject(
        android.os.Parcel parcel,
        android.os.Parcelable.Creator<T> c) {
      if (parcel.readInt() != 0) {
          return c.createFromParcel(parcel);
      } else {
          return null;
      }
    }
    static private <T extends android.os.Parcelable> void writeTypedObject(
        android.os.Parcel parcel, T value, int parcelableFlags) {
      if (value != null) {
        parcel.writeInt(1);
        value.writeToParcel(parcel, parcelableFlags);
      } else {
        parcel.writeInt(0);
      }
    }
  }
}

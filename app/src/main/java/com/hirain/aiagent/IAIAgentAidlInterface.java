/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: D:\code\android\forSdk\Sdk\build-tools\35.0.0\aidl.exe -pD:\code\android\forSdk\Sdk\platforms\android-35\framework.aidl -oD:\code\android\AndroidStudioProjects\AIAgent\app\build\generated\aidl_source_output_dir\debug\out -ID:\code\android\AndroidStudioProjects\AIAgent\app\src\main\aidl -ID:\code\android\AndroidStudioProjects\AIAgent\app\src\debug\aidl -IC:\Users\yala5\.gradle\caches\8.11.1\transforms\7dee0c696b5f4f8c3ab765b971151a0f\transformed\core-1.13.0\aidl -IC:\Users\yala5\.gradle\caches\8.11.1\transforms\5ccdbc8ad7d7d7dc36f9eb6d515eaabe\transformed\versionedparcelable-1.1.1\aidl -dC:\Users\yala5\AppData\Local\Temp\aidl10404203544354527115.d D:\code\android\AndroidStudioProjects\AIAgent\app\src\main\aidl\com\hirain\aiagent\IAIAgentAidlInterface.aidl
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
    static final int TRANSACTION_registerListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_unregisterListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "com.hirain.aiagent.IAIAgentAidlInterface";
  public void processAgentRequest(com.hirain.aiagent.AgentRequest request) throws android.os.RemoteException;
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

/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: D:\code\android\forSdk\Sdk\build-tools\35.0.0\aidl.exe -pD:\code\android\forSdk\Sdk\platforms\android-35\framework.aidl -oD:\code\android\AndroidStudioProjects\AIAgent\app\build\generated\aidl_source_output_dir\debug\out -ID:\code\android\AndroidStudioProjects\AIAgent\app\src\main\aidl -ID:\code\android\AndroidStudioProjects\AIAgent\app\src\debug\aidl -IC:\Users\yala5\.gradle\caches\8.11.1\transforms\7dee0c696b5f4f8c3ab765b971151a0f\transformed\core-1.13.0\aidl -IC:\Users\yala5\.gradle\caches\8.11.1\transforms\5ccdbc8ad7d7d7dc36f9eb6d515eaabe\transformed\versionedparcelable-1.1.1\aidl -dC:\Users\yala5\AppData\Local\Temp\aidl607387764507733999.d D:\code\android\AndroidStudioProjects\AIAgent\app\src\main\aidl\com\hirain\aiagent\IAIAgentAidlListener.aidl
 */
package com.hirain.aiagent;
public interface IAIAgentAidlListener extends android.os.IInterface
{
  /** Default implementation for IAIAgentAidlListener. */
  public static class Default implements com.hirain.aiagent.IAIAgentAidlListener
  {
    @Override public void onAIResponse(com.hirain.aiagent.AgentResponse response) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.hirain.aiagent.IAIAgentAidlListener
  {
    /** Construct the stub at attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.hirain.aiagent.IAIAgentAidlListener interface,
     * generating a proxy if needed.
     */
    public static com.hirain.aiagent.IAIAgentAidlListener asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.hirain.aiagent.IAIAgentAidlListener))) {
        return ((com.hirain.aiagent.IAIAgentAidlListener)iin);
      }
      return new com.hirain.aiagent.IAIAgentAidlListener.Stub.Proxy(obj);
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
        case TRANSACTION_onAIResponse:
        {
          com.hirain.aiagent.AgentResponse _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.hirain.aiagent.AgentResponse.CREATOR);
          this.onAIResponse(_arg0);
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
    private static class Proxy implements com.hirain.aiagent.IAIAgentAidlListener
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
      @Override public void onAIResponse(com.hirain.aiagent.AgentResponse response) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, response, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAIResponse, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onAIResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "com.hirain.aiagent.IAIAgentAidlListener";
  public void onAIResponse(com.hirain.aiagent.AgentResponse response) throws android.os.RemoteException;
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

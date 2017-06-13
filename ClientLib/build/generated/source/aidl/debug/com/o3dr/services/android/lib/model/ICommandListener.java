/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Users\\liangt\\Documents\\dronekit-android-dronekit-android-v3.0.2\\ClientLib\\src\\main\\java\\com\\o3dr\\services\\android\\lib\\model\\ICommandListener.aidl
 */
package com.o3dr.services.android.lib.model;
/**
* Asynchronous notification of a command execution state.
*/
public interface ICommandListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.o3dr.services.android.lib.model.ICommandListener
{
private static final java.lang.String DESCRIPTOR = "com.o3dr.services.android.lib.model.ICommandListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.o3dr.services.android.lib.model.ICommandListener interface,
 * generating a proxy if needed.
 */
public static com.o3dr.services.android.lib.model.ICommandListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.o3dr.services.android.lib.model.ICommandListener))) {
return ((com.o3dr.services.android.lib.model.ICommandListener)iin);
}
return new com.o3dr.services.android.lib.model.ICommandListener.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_onSuccess:
{
data.enforceInterface(DESCRIPTOR);
this.onSuccess();
return true;
}
case TRANSACTION_onError:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.onError(_arg0);
return true;
}
case TRANSACTION_onTimeout:
{
data.enforceInterface(DESCRIPTOR);
this.onTimeout();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.o3dr.services.android.lib.model.ICommandListener
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
/**
    * Called when the command was executed successfully.
    */
@Override public void onSuccess() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onSuccess, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
/**
    * Called when the command execution failed.
    * @param executionError Defined by {@link com.o3dr.services.android.lib.drone.attribute.error.CommandExecutionError}
    */
@Override public void onError(int executionError) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(executionError);
mRemote.transact(Stub.TRANSACTION_onError, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
/**
    * Called when the command execution times out.
    */
@Override public void onTimeout() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onTimeout, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_onSuccess = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_onTimeout = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
}
/**
    * Called when the command was executed successfully.
    */
public void onSuccess() throws android.os.RemoteException;
/**
    * Called when the command execution failed.
    * @param executionError Defined by {@link com.o3dr.services.android.lib.drone.attribute.error.CommandExecutionError}
    */
public void onError(int executionError) throws android.os.RemoteException;
/**
    * Called when the command execution times out.
    */
public void onTimeout() throws android.os.RemoteException;
}

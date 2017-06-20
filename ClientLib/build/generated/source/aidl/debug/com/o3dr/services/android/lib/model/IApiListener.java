/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\withSIM\\starl\\ClientLib\\src\\main\\java\\com\\o3dr\\services\\android\\lib\\model\\IApiListener.aidl
 */
package com.o3dr.services.android.lib.model;
/**
* DroneAPI event listener. A valid instance must be provided at api registration.
*/
public interface IApiListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.o3dr.services.android.lib.model.IApiListener
{
private static final java.lang.String DESCRIPTOR = "com.o3dr.services.android.lib.model.IApiListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.o3dr.services.android.lib.model.IApiListener interface,
 * generating a proxy if needed.
 */
public static com.o3dr.services.android.lib.model.IApiListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.o3dr.services.android.lib.model.IApiListener))) {
return ((com.o3dr.services.android.lib.model.IApiListener)iin);
}
return new com.o3dr.services.android.lib.model.IApiListener.Stub.Proxy(obj);
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
case TRANSACTION_getApiVersionCode:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getApiVersionCode();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_onConnectionFailed:
{
data.enforceInterface(DESCRIPTOR);
com.o3dr.services.android.lib.drone.connection.ConnectionResult _arg0;
if ((0!=data.readInt())) {
_arg0 = com.o3dr.services.android.lib.drone.connection.ConnectionResult.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.onConnectionFailed(_arg0);
return true;
}
case TRANSACTION_getClientVersionCode:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getClientVersionCode();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.o3dr.services.android.lib.model.IApiListener
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
@Override public int getApiVersionCode() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getApiVersionCode, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
    * @deprecated
    * Called when the connection attempt fails.
    * @param result Describe why the connection failed.
    */
@Override public void onConnectionFailed(com.o3dr.services.android.lib.drone.connection.ConnectionResult result) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((result!=null)) {
_data.writeInt(1);
result.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_onConnectionFailed, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public int getClientVersionCode() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getClientVersionCode, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_getApiVersionCode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onConnectionFailed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_getClientVersionCode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
}
public int getApiVersionCode() throws android.os.RemoteException;
/**
    * @deprecated
    * Called when the connection attempt fails.
    * @param result Describe why the connection failed.
    */
public void onConnectionFailed(com.o3dr.services.android.lib.drone.connection.ConnectionResult result) throws android.os.RemoteException;
public int getClientVersionCode() throws android.os.RemoteException;
}

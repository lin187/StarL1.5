/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Users\\alexaad1\\Documents\\starl\\ClientLib\\src\\main\\java\\com\\o3dr\\services\\android\\lib\\model\\IMavlinkObserver.aidl
 */
package com.o3dr.services.android.lib.model;
/**
* Asynchronous notification on receipt of new mavlink message.
*/
public interface IMavlinkObserver extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.o3dr.services.android.lib.model.IMavlinkObserver
{
private static final java.lang.String DESCRIPTOR = "com.o3dr.services.android.lib.model.IMavlinkObserver";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.o3dr.services.android.lib.model.IMavlinkObserver interface,
 * generating a proxy if needed.
 */
public static com.o3dr.services.android.lib.model.IMavlinkObserver asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.o3dr.services.android.lib.model.IMavlinkObserver))) {
return ((com.o3dr.services.android.lib.model.IMavlinkObserver)iin);
}
return new com.o3dr.services.android.lib.model.IMavlinkObserver.Stub.Proxy(obj);
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
case TRANSACTION_onMavlinkMessageReceived:
{
data.enforceInterface(DESCRIPTOR);
com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper _arg0;
if ((0!=data.readInt())) {
_arg0 = com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.onMavlinkMessageReceived(_arg0);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.o3dr.services.android.lib.model.IMavlinkObserver
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
    * Notify observer that a mavlink message was received.
    * @param messageWrapper Wrapper for the received mavlink message.
    */
@Override public void onMavlinkMessageReceived(com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper messageWrapper) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((messageWrapper!=null)) {
_data.writeInt(1);
messageWrapper.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_onMavlinkMessageReceived, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_onMavlinkMessageReceived = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
/**
    * Notify observer that a mavlink message was received.
    * @param messageWrapper Wrapper for the received mavlink message.
    */
public void onMavlinkMessageReceived(com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper messageWrapper) throws android.os.RemoteException;
}

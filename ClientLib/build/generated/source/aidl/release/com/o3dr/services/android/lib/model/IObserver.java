/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Users\\liangt\\Documents\\GitHub\\starl\\ClientLib\\src\\main\\java\\com\\o3dr\\services\\android\\lib\\model\\IObserver.aidl
 */
package com.o3dr.services.android.lib.model;
/**
* Asynchronous notification on change of vehicle state is available by registering observers for
* attribute changes.
*/
public interface IObserver extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.o3dr.services.android.lib.model.IObserver
{
private static final java.lang.String DESCRIPTOR = "com.o3dr.services.android.lib.model.IObserver";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.o3dr.services.android.lib.model.IObserver interface,
 * generating a proxy if needed.
 */
public static com.o3dr.services.android.lib.model.IObserver asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.o3dr.services.android.lib.model.IObserver))) {
return ((com.o3dr.services.android.lib.model.IObserver)iin);
}
return new com.o3dr.services.android.lib.model.IObserver.Stub.Proxy(obj);
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
case TRANSACTION_onAttributeUpdated:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
android.os.Bundle _arg1;
if ((0!=data.readInt())) {
_arg1 = android.os.Bundle.CREATOR.createFromParcel(data);
}
else {
_arg1 = null;
}
this.onAttributeUpdated(_arg0, _arg1);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.o3dr.services.android.lib.model.IObserver
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
    * Notify observer that the named attribute has changed.
    * @param attributeEvent event describing the update. The supported events are listed in {@link com.o3dr.services.android.lib.drone.attribute.AttributeEvent}
    * @param attributeBundle bundle object from which additional event data can be retrieved.
    */
@Override public void onAttributeUpdated(java.lang.String attributeEvent, android.os.Bundle eventExtras) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(attributeEvent);
if ((eventExtras!=null)) {
_data.writeInt(1);
eventExtras.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_onAttributeUpdated, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_onAttributeUpdated = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
/**
    * Notify observer that the named attribute has changed.
    * @param attributeEvent event describing the update. The supported events are listed in {@link com.o3dr.services.android.lib.drone.attribute.AttributeEvent}
    * @param attributeBundle bundle object from which additional event data can be retrieved.
    */
public void onAttributeUpdated(java.lang.String attributeEvent, android.os.Bundle eventExtras) throws android.os.RemoteException;
}

/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\6-9Starl\\starl\\ClientLib\\src\\main\\java\\com\\o3dr\\services\\android\\lib\\model\\IDroneApi.aidl
 */
package com.o3dr.services.android.lib.model;
/**
* Interface used to access the drone properties.
*/
public interface IDroneApi extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.o3dr.services.android.lib.model.IDroneApi
{
private static final java.lang.String DESCRIPTOR = "com.o3dr.services.android.lib.model.IDroneApi";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.o3dr.services.android.lib.model.IDroneApi interface,
 * generating a proxy if needed.
 */
public static com.o3dr.services.android.lib.model.IDroneApi asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.o3dr.services.android.lib.model.IDroneApi))) {
return ((com.o3dr.services.android.lib.model.IDroneApi)iin);
}
return new com.o3dr.services.android.lib.model.IDroneApi.Stub.Proxy(obj);
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
case TRANSACTION_getAttribute:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
android.os.Bundle _result = this.getAttribute(_arg0);
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_performAction:
{
data.enforceInterface(DESCRIPTOR);
com.o3dr.services.android.lib.model.action.Action _arg0;
if ((0!=data.readInt())) {
_arg0 = com.o3dr.services.android.lib.model.action.Action.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.performAction(_arg0);
reply.writeNoException();
if ((_arg0!=null)) {
reply.writeInt(1);
_arg0.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_performAsyncAction:
{
data.enforceInterface(DESCRIPTOR);
com.o3dr.services.android.lib.model.action.Action _arg0;
if ((0!=data.readInt())) {
_arg0 = com.o3dr.services.android.lib.model.action.Action.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.performAsyncAction(_arg0);
return true;
}
case TRANSACTION_addAttributesObserver:
{
data.enforceInterface(DESCRIPTOR);
com.o3dr.services.android.lib.model.IObserver _arg0;
_arg0 = com.o3dr.services.android.lib.model.IObserver.Stub.asInterface(data.readStrongBinder());
this.addAttributesObserver(_arg0);
return true;
}
case TRANSACTION_removeAttributesObserver:
{
data.enforceInterface(DESCRIPTOR);
com.o3dr.services.android.lib.model.IObserver _arg0;
_arg0 = com.o3dr.services.android.lib.model.IObserver.Stub.asInterface(data.readStrongBinder());
this.removeAttributesObserver(_arg0);
return true;
}
case TRANSACTION_addMavlinkObserver:
{
data.enforceInterface(DESCRIPTOR);
com.o3dr.services.android.lib.model.IMavlinkObserver _arg0;
_arg0 = com.o3dr.services.android.lib.model.IMavlinkObserver.Stub.asInterface(data.readStrongBinder());
this.addMavlinkObserver(_arg0);
return true;
}
case TRANSACTION_removeMavlinkObserver:
{
data.enforceInterface(DESCRIPTOR);
com.o3dr.services.android.lib.model.IMavlinkObserver _arg0;
_arg0 = com.o3dr.services.android.lib.model.IMavlinkObserver.Stub.asInterface(data.readStrongBinder());
this.removeMavlinkObserver(_arg0);
return true;
}
case TRANSACTION_executeAction:
{
data.enforceInterface(DESCRIPTOR);
com.o3dr.services.android.lib.model.action.Action _arg0;
if ((0!=data.readInt())) {
_arg0 = com.o3dr.services.android.lib.model.action.Action.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
com.o3dr.services.android.lib.model.ICommandListener _arg1;
_arg1 = com.o3dr.services.android.lib.model.ICommandListener.Stub.asInterface(data.readStrongBinder());
this.executeAction(_arg0, _arg1);
reply.writeNoException();
if ((_arg0!=null)) {
reply.writeInt(1);
_arg0.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_executeAsyncAction:
{
data.enforceInterface(DESCRIPTOR);
com.o3dr.services.android.lib.model.action.Action _arg0;
if ((0!=data.readInt())) {
_arg0 = com.o3dr.services.android.lib.model.action.Action.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
com.o3dr.services.android.lib.model.ICommandListener _arg1;
_arg1 = com.o3dr.services.android.lib.model.ICommandListener.Stub.asInterface(data.readStrongBinder());
this.executeAsyncAction(_arg0, _arg1);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.o3dr.services.android.lib.model.IDroneApi
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
    * Retrieves the attribute whose type is specified by the parameter.
    * @param attributeType type of the attribute to retrieve. The list of supported
                        types is stored in {@link com.o3dr.services.android.lib.drone.attribute.AttributeType}.
    * @return Bundle object containing the requested attribute.
    */
@Override public android.os.Bundle getAttribute(java.lang.String attributeType) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
android.os.Bundle _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(attributeType);
mRemote.transact(Stub.TRANSACTION_getAttribute, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = android.os.Bundle.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
    * Performs an action among the set exposed by the api.
    * @param action Action to perform.
    */
@Override public void performAction(com.o3dr.services.android.lib.model.action.Action action) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((action!=null)) {
_data.writeInt(1);
action.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_performAction, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
action.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
/*** Oneway method calls ***//**
    * Performs asynchronously an action among the set exposed by the api.
    * @param action Action to perform.
    */
@Override public void performAsyncAction(com.o3dr.services.android.lib.model.action.Action action) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((action!=null)) {
_data.writeInt(1);
action.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_performAsyncAction, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
/**
    * Register a listener to receive drone events.
    * @param observer the observer to register.
    */
@Override public void addAttributesObserver(com.o3dr.services.android.lib.model.IObserver observer) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((observer!=null))?(observer.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_addAttributesObserver, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
/**
    * Removes a drone events listener.
    * @param observer the observer to remove.
    */
@Override public void removeAttributesObserver(com.o3dr.services.android.lib.model.IObserver observer) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((observer!=null))?(observer.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_removeAttributesObserver, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
/**
    * Register a listener to receive mavlink messages.
    * @param observer the observer to register.
    */
@Override public void addMavlinkObserver(com.o3dr.services.android.lib.model.IMavlinkObserver observer) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((observer!=null))?(observer.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_addMavlinkObserver, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
/**
    * Removes a mavlink message listener.
    * @param observer the observer to remove.
    */
@Override public void removeMavlinkObserver(com.o3dr.services.android.lib.model.IMavlinkObserver observer) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((observer!=null))?(observer.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_removeMavlinkObserver, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
/**
    * Performs an action among the set exposed by the api.
    * @param action Action to perform.
    */
@Override public void executeAction(com.o3dr.services.android.lib.model.action.Action action, com.o3dr.services.android.lib.model.ICommandListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((action!=null)) {
_data.writeInt(1);
action.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_executeAction, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
action.readFromParcel(_reply);
}
}
finally {
_reply.recycle();
_data.recycle();
}
}
/*** Oneway method calls ***//**
    * Performs asynchronously an action among the set exposed by the api.
    * @param action Action to perform.
    * @param listener Register a callback to be invoken when the action is executed.
    */
@Override public void executeAsyncAction(com.o3dr.services.android.lib.model.action.Action action, com.o3dr.services.android.lib.model.ICommandListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((action!=null)) {
_data.writeInt(1);
action.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_executeAsyncAction, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_getAttribute = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_performAction = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_performAsyncAction = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_addAttributesObserver = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_removeAttributesObserver = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_addMavlinkObserver = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_removeMavlinkObserver = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_executeAction = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_executeAsyncAction = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
}
/**
    * Retrieves the attribute whose type is specified by the parameter.
    * @param attributeType type of the attribute to retrieve. The list of supported
                        types is stored in {@link com.o3dr.services.android.lib.drone.attribute.AttributeType}.
    * @return Bundle object containing the requested attribute.
    */
public android.os.Bundle getAttribute(java.lang.String attributeType) throws android.os.RemoteException;
/**
    * Performs an action among the set exposed by the api.
    * @param action Action to perform.
    */
public void performAction(com.o3dr.services.android.lib.model.action.Action action) throws android.os.RemoteException;
/*** Oneway method calls ***//**
    * Performs asynchronously an action among the set exposed by the api.
    * @param action Action to perform.
    */
public void performAsyncAction(com.o3dr.services.android.lib.model.action.Action action) throws android.os.RemoteException;
/**
    * Register a listener to receive drone events.
    * @param observer the observer to register.
    */
public void addAttributesObserver(com.o3dr.services.android.lib.model.IObserver observer) throws android.os.RemoteException;
/**
    * Removes a drone events listener.
    * @param observer the observer to remove.
    */
public void removeAttributesObserver(com.o3dr.services.android.lib.model.IObserver observer) throws android.os.RemoteException;
/**
    * Register a listener to receive mavlink messages.
    * @param observer the observer to register.
    */
public void addMavlinkObserver(com.o3dr.services.android.lib.model.IMavlinkObserver observer) throws android.os.RemoteException;
/**
    * Removes a mavlink message listener.
    * @param observer the observer to remove.
    */
public void removeMavlinkObserver(com.o3dr.services.android.lib.model.IMavlinkObserver observer) throws android.os.RemoteException;
/**
    * Performs an action among the set exposed by the api.
    * @param action Action to perform.
    */
public void executeAction(com.o3dr.services.android.lib.model.action.Action action, com.o3dr.services.android.lib.model.ICommandListener listener) throws android.os.RemoteException;
/*** Oneway method calls ***//**
    * Performs asynchronously an action among the set exposed by the api.
    * @param action Action to perform.
    * @param listener Register a callback to be invoken when the action is executed.
    */
public void executeAsyncAction(com.o3dr.services.android.lib.model.action.Action action, com.o3dr.services.android.lib.model.ICommandListener listener) throws android.os.RemoteException;
}

/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Users\\alexaad1\\Documents\\starl\\ClientLib\\src\\main\\java\\com\\o3dr\\services\\android\\lib\\model\\IDroidPlannerServices.aidl
 */
package com.o3dr.services.android.lib.model;
/**
* Used to establish connection with a drone.
*/
public interface IDroidPlannerServices extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.o3dr.services.android.lib.model.IDroidPlannerServices
{
private static final java.lang.String DESCRIPTOR = "com.o3dr.services.android.lib.model.IDroidPlannerServices";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.o3dr.services.android.lib.model.IDroidPlannerServices interface,
 * generating a proxy if needed.
 */
public static com.o3dr.services.android.lib.model.IDroidPlannerServices asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.o3dr.services.android.lib.model.IDroidPlannerServices))) {
return ((com.o3dr.services.android.lib.model.IDroidPlannerServices)iin);
}
return new com.o3dr.services.android.lib.model.IDroidPlannerServices.Stub.Proxy(obj);
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
case TRANSACTION_getServiceVersionCode:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getServiceVersionCode();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_releaseDroneApi:
{
data.enforceInterface(DESCRIPTOR);
com.o3dr.services.android.lib.model.IDroneApi _arg0;
_arg0 = com.o3dr.services.android.lib.model.IDroneApi.Stub.asInterface(data.readStrongBinder());
this.releaseDroneApi(_arg0);
reply.writeNoException();
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
case TRANSACTION_registerDroneApi:
{
data.enforceInterface(DESCRIPTOR);
com.o3dr.services.android.lib.model.IApiListener _arg0;
_arg0 = com.o3dr.services.android.lib.model.IApiListener.Stub.asInterface(data.readStrongBinder());
java.lang.String _arg1;
_arg1 = data.readString();
com.o3dr.services.android.lib.model.IDroneApi _result = this.registerDroneApi(_arg0, _arg1);
reply.writeNoException();
reply.writeStrongBinder((((_result!=null))?(_result.asBinder()):(null)));
return true;
}
case TRANSACTION_getConnectedApps:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
android.os.Bundle[] _result = this.getConnectedApps(_arg0);
reply.writeNoException();
reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.o3dr.services.android.lib.model.IDroidPlannerServices
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
@Override public int getServiceVersionCode() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getServiceVersionCode, _data, _reply, 0);
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
    * Release the handle to the droidplanner api.
    *
    * @param callback callback used to receive drone api events.
    */
@Override public void releaseDroneApi(com.o3dr.services.android.lib.model.IDroneApi droneApi) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((droneApi!=null))?(droneApi.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_releaseDroneApi, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
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
    * Acquire an handle to the droidplanner api.
    * @param listener listener for the DroneAPI events.
    * @param appId application id for the application acquiring the drone api handle.
    * @return IDroneApi object used to interact with the drone.
    */
@Override public com.o3dr.services.android.lib.model.IDroneApi registerDroneApi(com.o3dr.services.android.lib.model.IApiListener listener, java.lang.String appId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
com.o3dr.services.android.lib.model.IDroneApi _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
_data.writeString(appId);
mRemote.transact(Stub.TRANSACTION_registerDroneApi, _data, _reply, 0);
_reply.readException();
_result = com.o3dr.services.android.lib.model.IDroneApi.Stub.asInterface(_reply.readStrongBinder());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
    * Retrieve the list of all the connected apps.
    * The bundles in the returned array contains the appId and connection parameter of the connected apps.
    * @param requesterId id for the application requesting the information.
    */
@Override public android.os.Bundle[] getConnectedApps(java.lang.String requesterId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
android.os.Bundle[] _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(requesterId);
mRemote.transact(Stub.TRANSACTION_getConnectedApps, _data, _reply, 0);
_reply.readException();
_result = _reply.createTypedArray(android.os.Bundle.CREATOR);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_getServiceVersionCode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_releaseDroneApi = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_getApiVersionCode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_registerDroneApi = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_getConnectedApps = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
}
public int getServiceVersionCode() throws android.os.RemoteException;
/**
    * Release the handle to the droidplanner api.
    *
    * @param callback callback used to receive drone api events.
    */
public void releaseDroneApi(com.o3dr.services.android.lib.model.IDroneApi droneApi) throws android.os.RemoteException;
public int getApiVersionCode() throws android.os.RemoteException;
/**
    * Acquire an handle to the droidplanner api.
    * @param listener listener for the DroneAPI events.
    * @param appId application id for the application acquiring the drone api handle.
    * @return IDroneApi object used to interact with the drone.
    */
public com.o3dr.services.android.lib.model.IDroneApi registerDroneApi(com.o3dr.services.android.lib.model.IApiListener listener, java.lang.String appId) throws android.os.RemoteException;
/**
    * Retrieve the list of all the connected apps.
    * The bundles in the returned array contains the appId and connection parameter of the connected apps.
    * @param requesterId id for the application requesting the information.
    */
public android.os.Bundle[] getConnectedApps(java.lang.String requesterId) throws android.os.RemoteException;
}

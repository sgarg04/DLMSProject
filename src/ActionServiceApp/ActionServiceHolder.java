package ActionServiceApp;

/**
* ActionServiceApp/ActionServiceHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from ActionService.idl
* Saturday, March 30, 2019 5:33:58 PM EDT
*/

public final class ActionServiceHolder implements org.omg.CORBA.portable.Streamable
{
  public ActionServiceApp.ActionService value = null;

  public ActionServiceHolder ()
  {
  }

  public ActionServiceHolder (ActionServiceApp.ActionService initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = ActionServiceApp.ActionServiceHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    ActionServiceApp.ActionServiceHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return ActionServiceApp.ActionServiceHelper.type ();
  }

}

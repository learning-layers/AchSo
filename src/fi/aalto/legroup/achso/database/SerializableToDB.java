package fi.aalto.legroup.achso.database;

public interface SerializableToDB {
    /* Dummy interface for classes that can be serialized into database.
       Dialog class uses this to allow passing in arbitrary objects that have a representation
       in a database and will be modified through a dialog.
       For example getTextSetterDialog uses this interface to allow both SemanticVideo objects and
       Annotation objects to be passed in.

        TODO: Add getId() and getContentValues() to this interface, as they actually do belong here.
     */
}
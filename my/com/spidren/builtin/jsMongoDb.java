package com.spidren.builtin;

import com.eclipsesource.v8.*;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.*;
import org.bson.conversions.Bson;

import javax.management.Query;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class jsMongoDb implements JavaCallback{

    V8 v8;
    public static MongoClient mongoClient;
    public jsMongoDb(V8 v8) {
        this.v8 = v8;
        Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
        mongoLogger.setLevel(Level.SEVERE);
        mongoClient = new MongoClient( "localhost" , 27017 );
    }

    @Override
    public Object invoke(V8Object receiver, V8Array parameters) {
        String db = "";
        if (parameters.length() > 0) {
            Object arg1 = parameters.get(0);
            db = arg1.toString();
            if (arg1 instanceof Releasable) {
                ((Releasable) arg1).release();
            }
        }
        MongoDatabase database = mongoClient.getDatabase(db);
        V8Object va = new V8Object(v8);
        DB qr = new DB(database,v8,va);
        return va;
    }

    class DB {
        MongoDatabase db;
        public DB(MongoDatabase db, V8 v8, V8Object va) {
            this.db = db;
            BsonDocument bs = new BsonDocument();
            bs.append("find", new BsonString("customer"));
            //bs.append("filter",   Filters.text("{ _id: { $lt: 4 } }").toBsonDocument(BsonDocument.class,MongoClient.getDefaultCodecRegistry()));
            bs.append("filter",  Filters.lt("_id","1").toBsonDocument(BsonDocument.class,MongoClient.getDefaultCodecRegistry()));
            Document data = db.runCommand(bs);
            System.out.println(data.toJson());

            va.registerJavaMethod(this, "getCollection", "getCollection",new Class<?>[] {String.class}, false);
        }

        public Object getCollection(String str) {
            V8Object va = new V8Object(v8);
            MongoCollection customer = db.getCollection(str);
            Collection qr = new Collection(customer,v8,va);
            return va;
        }
    }

    class Collection {

        MongoCollection colection;
        public Collection(MongoCollection colection, V8 v8, V8Object va) {
            this.colection = colection;
            va.registerJavaMethod(this, "find", "find",new Class<?>[] {String.class}, false);
            va.registerJavaMethod(this, "insertOne", "insertOne",new Class<?>[] {String.class}, false);
            //va.registerJavaMethod(this, "findAndModify", "findAndModify",new Class<?>[] {String.class}, false);
        }

        public Object find(String str) {
            final StringBuilder sb = new StringBuilder();
            //Document myDoc = Document.parse("{ _id: { $lt: 4 } }");
            Document myDoc = Document.parse(str);
            FindIterable<Document> cursor = colection.find(myDoc);
            sb.append("[");
            cursor.forEach(new Block<Document>() {
                @Override
                public void apply(final Document document) {
                    sb.append(document.toJson()+",\n");
                }
            });
            sb.append("]");

            /*Document data = colection.runCommand(new Document("find", "{ _id: { $lt: 4 }"));
            System.out.println(data.toJson());*/

            return sb.toString();
        }

        public  void insertOne(String str) throws Exception {
            Document myDoc = Document.parse(str);
            colection.insertOne(myDoc);
        }

        public  void insertUpdate(String str) throws Exception {
            Document myDoc = Document.parse(str);
            //colection.updateMany();
        }

        public  void findOneAndUpdate(String str) throws Exception {
            //Document myDoc = Document.parse(str);
           // colection.findOneAndUpdate(Filters.lt('0'));
        }

    }
}

using MongoDB.Driver;

namespace TradeDemoBackend.Utils.Mongo;

public interface IMongoDbClientFactory
{
    IMongoClient GetClient();

    IMongoCollection<T> GetCollection<T>(string collection);
}
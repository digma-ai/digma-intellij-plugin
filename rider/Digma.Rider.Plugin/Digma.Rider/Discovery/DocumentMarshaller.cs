using Digma.Rider.Protocol;
using JetBrains.Rd;
using JetBrains.Serialization;
using JetBrains.Util.PersistentMap;

namespace Digma.Rider.Discovery
{
    internal class DocumentMarshaller : IUnsafeMarshaller<RiderDocumentInfo>
    {
        public void Marshal(UnsafeWriter writer, RiderDocumentInfo value)
        {
            RiderDocumentInfo.Write(new SerializationCtx(), writer, value);
            // value.RdId.Write(writer);
            // writer.Write(value.IsComplete);
            // writer.Write(value.FileUri);
            // writer.Write(UnsafeWriter.StringDelegate, RiderMethodInfoWriteDelegate, value.Methods);
        }

        public RiderDocumentInfo Unmarshal(UnsafeReader reader)
        {
            return RiderDocumentInfo.Read(new SerializationCtx(), reader);
            
            // var id = RdId.Read(reader);
            // var isComplete = reader.ReadBool();
            // var path = reader.ReadString();
            // IDictionary<string, RiderMethodInfo> methods = reader.ReadDictionary(UnsafeReader.StringDelegate,
            //     RiderMethodInfoReadDelegate, _ => new Dictionary<string, RiderMethodInfo>());
            // var document = new RiderDocumentInfo(isComplete,path ?? throw new InvalidOperationException("Document path is null"),new List<RiderMethodInfo>());
            // document.RdId = id;
            // Debug.Assert(methods != null, nameof(methods) + " != null");
            // foreach (var riderMethodInfo in methods)
            // {
            //     document.Methods.Add(riderMethodInfo.Key, riderMethodInfo.Value);
            // }
            //
            // return document;
        }


        // private static readonly UnsafeWriter.WriteDelegate<RiderMethodInfo> RiderMethodInfoWriteDelegate =
        //     (writer, x) => WriteRiderMethodInfo(writer, x);
        //
        // private static void WriteRiderMethodInfo(UnsafeWriter writer, RiderMethodInfo value)
        // {
        //     SerializationCtx ctx = new SerializationCtx();
        //     RiderMethodInfo.Write(ctx, writer, value);
        // }
        //
        //
        // private static readonly UnsafeReader.ReadDelegate<RiderMethodInfo> RiderMethodInfoReadDelegate =
        //     reader => ReadRiderMethodInfo(reader);
        //
        // private static RiderMethodInfo ReadRiderMethodInfo(UnsafeReader reader)
        // {
        //     SerializationCtx ctx = new SerializationCtx();
        //     return RiderMethodInfo.Read(ctx, reader);
        // }
    }
}
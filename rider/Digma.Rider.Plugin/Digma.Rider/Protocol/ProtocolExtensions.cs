using System.Linq;
using NuGet;

namespace Digma.Rider.Protocol
{
    public static class ProtocolModelExtensions
    {
        //Equals for Digma.Rider.Protocol.Document.
        // the RiderMethodInfo elements in Methods already have Equals.
        // the name is CheckEquals because can't override Equals in extension.
        public static bool CheckEquals(this RiderDocumentInfo document,RiderDocumentInfo other)
        {
            if (document == null && other == null)
                return true;
            if (other == null)
                return false;
            if (document == null) //satisfy resharper
                return false;
            
            return document.FileUri.Equals(other.FileUri) &&
                   document.Methods.Count == other.Methods.Count &&
                   !document.Methods.Except(other.Methods).Any();
        }
        
        
      
        
        public static bool HasCodeObjects(this RiderDocumentInfo document)
        {
            return !document.Methods.IsEmpty();
        }
    }
}
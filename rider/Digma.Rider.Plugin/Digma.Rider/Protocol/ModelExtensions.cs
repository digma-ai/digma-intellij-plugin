using System.Linq;

namespace Digma.Rider.Protocol
{
    public static class ModelExtensions
    {
        //Equals for Digma.Rider.Protocol.Document.
        // the RiderMethodInfo elements in Methods already have Equals.
        // the name is CheckEquals because can't override Equals in extension.
        public static bool CheckEquals(this Document document,Document other)
        {
            if (document == null && other == null)
                return true;
            if (other == null)
                return false;
            if (document == null) //satisfy resharper
                return false;
            
            return document.Path.Equals(other.Path) &&
                   document.Methods.Count == other.Methods.Count &&
                   !document.Methods.Except(other.Methods).Any();
        }
    }
}
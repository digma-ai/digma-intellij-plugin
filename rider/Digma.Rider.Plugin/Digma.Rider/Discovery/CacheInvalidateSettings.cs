using System;
using JetBrains.Application.Communication;
using JetBrains.Application.Settings;
using JetBrains.Application.Settings.WellKnownRootKeys;

namespace Digma.Rider.Discovery
{

    /// <summary>
    /// if any of the types that we keep in the cache is changed then a rebuild is necessary,otherwise unmarshalling
    /// will fail.
    /// This is resharper settings to help force invalidate for CodeObjectsCache that was saved before a certain date.
    /// its half manual, if the cached objects structure changes between versions then RequiredInvalidateTime
    /// needs to change. the date should be late enough so that everyone who installs the next version will be invalidated.
    /// for example if commiting the change at 2022-07-17T22:37:30 then that should be the RequiredInvalidateTime.
    /// LastInvalidateTime is updated by CodeObjectsCache.
    /// </summary>
    [SettingsKey(typeof(EnvironmentSettings), "CodeObjectsCache invalidate settings")]
    internal class CacheInvalidateSettings
    {

        //update this field if the structure of the cache changes.
        public string RequiredInvalidateTime = "2022-07-17T23:04:30";

        [SettingsEntry(null, "Last invalidate time")]
        public string LastInvalidateTime { get; set; }
    }

}
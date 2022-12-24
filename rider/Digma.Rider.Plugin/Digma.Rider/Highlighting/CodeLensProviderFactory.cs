using System.Collections.Generic;
using System.Linq;
using JetBrains.ProjectModel;

namespace Digma.Rider.Highlighting
{
    [SolutionComponent]
    public class CodeLensProviderFactory
    {
        private readonly ErrorHotspotMethodInsightsProvider _errorHotspotMethodInsightsProvider;
        private readonly UsageMethodInsightsProvider _usageMethodInsightsProvider;
        private readonly ScaleFactorMethodInsightsProvider _scaleFactorMethodInsightsProvider;
        private readonly SlowEndpointMethodInsightsProvider _slowEndpointMethodInsightsProvider;
        private readonly CodeLensMethodInsightsProvider1 _codeLensMethodInsightsProvider1;
        private readonly CodeLensMethodInsightsProvider2 _codeLensMethodInsightsProvider2;
        private readonly CodeLensMethodInsightsProvider3 _codeLensMethodInsightsProvider3;
        private readonly CodeLensMethodInsightsProvider4 _codeLensMethodInsightsProvider4;
        private readonly CodeLensMethodInsightsProvider5 _codeLensMethodInsightsProvider5;
        private readonly Dictionary<string, BaseMethodInsightsProvider> _genericProvidersMap = new();

        public CodeLensProviderFactory(
            ErrorHotspotMethodInsightsProvider errorHotspotMethodInsightsProvider,
            UsageMethodInsightsProvider usageMethodInsightsProvider,
            ScaleFactorMethodInsightsProvider scaleFactorMethodInsightsProvider,
            SlowEndpointMethodInsightsProvider slowEndpointMethodInsightsProvider,
            CodeLensMethodInsightsProvider1 codeLensMethodInsightsProvider1, 
            CodeLensMethodInsightsProvider2 codeLensMethodInsightsProvider2,
            CodeLensMethodInsightsProvider3 codeLensMethodInsightsProvider3,
            CodeLensMethodInsightsProvider4 codeLensMethodInsightsProvider4,
            CodeLensMethodInsightsProvider5 codeLensMethodInsightsProvider5
            )
        {
            _errorHotspotMethodInsightsProvider = errorHotspotMethodInsightsProvider;
            _usageMethodInsightsProvider = usageMethodInsightsProvider;
            _scaleFactorMethodInsightsProvider = scaleFactorMethodInsightsProvider;
            _slowEndpointMethodInsightsProvider = slowEndpointMethodInsightsProvider;
            _codeLensMethodInsightsProvider1 = codeLensMethodInsightsProvider1;
            _codeLensMethodInsightsProvider2 = codeLensMethodInsightsProvider2;
            _codeLensMethodInsightsProvider3 = codeLensMethodInsightsProvider3;
            _codeLensMethodInsightsProvider4 = codeLensMethodInsightsProvider4;
            _codeLensMethodInsightsProvider5 = codeLensMethodInsightsProvider5;
            InitializeGenericProvidersMap();
        }

        public BaseMethodInsightsProvider GetProvider(string lensTitle, List<string> usedGenericProviders)
        {
            if (lensTitle != null)
            {
                if (lensTitle.ToUpper().Contains("ERROR"))
                {
                    return _errorHotspotMethodInsightsProvider;    
                }
                if (lensTitle.ToUpper().Contains("USAGE"))
                {
                    return _usageMethodInsightsProvider;    
                }
                if (lensTitle.ToUpper().Contains("SCALE"))
                {
                    return _scaleFactorMethodInsightsProvider;    
                }
                if (lensTitle.ToUpper().Contains("SLOW ENDPOINT"))
                {
                    return _slowEndpointMethodInsightsProvider;
                }
            }
            return GetNotUsedGenericMethodInsightsProvider(usedGenericProviders); 
        }

        private BaseMethodInsightsProvider GetNotUsedGenericMethodInsightsProvider(ICollection<string> usedGenericProviders)
        {
            var availableProvidersKeys = _genericProvidersMap.Where(p => 
                    usedGenericProviders.All(p2 => !p2.Equals(p.Key)))
                .Select(x => x.Key).ToList();

            if (availableProvidersKeys.Count == 0)
            {
                // skip the code lens if all 5 generic available providers are used already
                return null;
            }
            var availableProviderKey = availableProvidersKeys.Count > 0 ? availableProvidersKeys[0] : _genericProvidersMap.First().Key;
            usedGenericProviders.Add(availableProviderKey);
            return _genericProvidersMap[availableProviderKey];
        }

        private void InitializeGenericProvidersMap()
        {
            _genericProvidersMap.Add(_codeLensMethodInsightsProvider1.ProviderId, _codeLensMethodInsightsProvider1);
            _genericProvidersMap.Add(_codeLensMethodInsightsProvider2.ProviderId, _codeLensMethodInsightsProvider2);
            _genericProvidersMap.Add(_codeLensMethodInsightsProvider3.ProviderId, _codeLensMethodInsightsProvider3);
            _genericProvidersMap.Add(_codeLensMethodInsightsProvider4.ProviderId, _codeLensMethodInsightsProvider4);
            _genericProvidersMap.Add(_codeLensMethodInsightsProvider5.ProviderId, _codeLensMethodInsightsProvider5);
        } 
    }
}
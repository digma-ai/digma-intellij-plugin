using JetBrains.Lifetimes;
using JetBrains.TextControl;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Util;

public class TextControlsLoggerUtil
{
    //just a utility for debugging text controls.
    //will register most listeners on text controls and log them.
    public TextControlsLoggerUtil(ITextControlManager textControlManager, ILogger logger, Lifetime lifetime)
    {
        textControlManager.VisibleTextControls.AddRemove.Advise(lifetime, h =>
        {
            if (h.IsAdding)
            {
                Log(logger, "VisibleTextControls AddRemove adding {0}", h.Value?.Document);
            }
            else
            {
                Log(logger, "VisibleTextControls AddRemove removing {0}", h.Value?.Document);
            }
        });
        textControlManager.VisibleTextControls.BeforeAddRemove.Advise(lifetime, h =>
        {
            if (h.IsAdding)
            {
                Log(logger, "VisibleTextControls BeforeAddRemove adding {0}", h.Value?.Document);
            }
            else
            {
                Log(logger, "VisibleTextControls BeforeAddRemove removing {0}", h.Value?.Document);
            }
        });

        textControlManager.TextControls.AddRemove.Advise(lifetime, h =>
        {
            if (h.IsAdding)
            {
                Log(logger, "TextControls AddRemove adding {0}", h.Value?.Document);
            }
            else
            {
                Log(logger, "TextControls AddRemove removing {0}", h.Value?.Document);
            }
        });
        textControlManager.TextControls.BeforeAddRemove.Advise(lifetime, h =>
        {
            if (h.IsAdding)
            {
                Log(logger, "TextControls BeforeAddRemove, adding {0}", h.Value?.Document);
            }
            else
            {
                Log(logger, "TextControls BeforeAddRemove removing {0}", h.Value?.Document);
            }
        });
        textControlManager.FocusedTextControlPerClient.BeforeAddRemove.Advise(lifetime, h =>
        {
            if (h.IsAdding)
            {
                Log(logger, "FocusedTextControlPerClient BeforeAddRemove adding key:{0},value:{1}", h.Value.Key,
                    h.Value.Value);
            }
            else
            {
                Log(logger, "FocusedTextControlPerClient BeforeAddRemove removing key:{0},value:{1}", h.Value.Key,
                    h.Value.Value);
            }
        });

        textControlManager.FocusedTextControlPerClient.AddRemove.Advise(lifetime, h =>
        {
            if (h.IsAdding)
            {
                Log(logger, "FocusedTextControlPerClient AddRemove adding key:{0},value:{1}", h.Value.Key,
                    h.Value.Value);
            }
            else
            {
                Log(logger, "FocusedTextControlPerClient AddRemove removing key:{0},value:{1}", h.Value.Key,
                    h.Value.Value);
            }
        });

        textControlManager.LastFocusedTextControlPerClient.AddRemove.Advise(lifetime, h =>
        {
            if (h.IsAdding)
            {
                Log(logger, "LastFocusedTextControlPerClient AddRemove adding key:{0},value:{1}", h.Value.Key,
                    h.Value.Value);
            }
            else
            {
                Log(logger, "LastFocusedTextControlPerClient AddRemove removing key:{0},value:{1}", h.Value.Key,
                    h.Value.Value);
            }
        });

        textControlManager.LastFocusedTextControlPerClient.BeforeAddRemove.Advise(lifetime, h =>
        {
            if (h.IsAdding)
            {
                Log(logger, "LastFocusedTextControlPerClient BeforeAddRemove adding key:{0},value:{1}", h.Value.Key,
                    h.Value.Value);
            }
            else
            {
                Log(logger, "LastFocusedTextControlPerClient BeforeAddRemove removing key:{0},value:{1}", h.Value.Key,
                    h.Value.Value);
            }
        });
    }
}
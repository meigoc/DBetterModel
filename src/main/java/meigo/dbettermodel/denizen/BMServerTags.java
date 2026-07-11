/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.denizen;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.tags.PseudoObjectTagBase;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import meigo.dbettermodel.DBetterModel;
import meigo.dbettermodel.compat.api.BmCapability;
import meigo.dbettermodel.compat.api.BmPlatform;

/** The <bm.*> server tag base. */
public class BMServerTags extends PseudoObjectTagBase<BMServerTags> {

    public static BMServerTags instance;

    public static void init() {
        instance = new BMServerTags();
        TagManager.registerTagHandler(BMServerTags.class, "bm", t -> instance);
    }

    @Override
    public void register() {

        // <--[tag]
        // @attribute <bm.version>
        // @returns ElementTag
        // @plugin DBetterModel
        // @description
        // Returns the version string of the installed BetterModel plugin.
        // -->
        tagProcessor.registerTag(ElementTag.class, "version", (attr, obj) -> {
            BmPlatform platform = DBetterModel.platform();
            return platform == null ? null : new ElementTag(platform.bmVersion());
        });

        // <--[tag]
        // @attribute <bm.compat_layer>
        // @returns ElementTag
        // @plugin DBetterModel
        // @description
        // Returns the active DBetterModel compat layer: 'v1' (BetterModel 1.15.x), 'v2' (2.x), or 'v3' (3.x+).
        // -->
        tagProcessor.registerTag(ElementTag.class, "compat_layer", (attr, obj) -> {
            BmPlatform platform = DBetterModel.platform();
            if (platform == null) return null;
            String pkg = platform.getClass().getPackageName();
            return new ElementTag(pkg.substring(pkg.lastIndexOf('.') + 1));
        });

        // <--[tag]
        // @attribute <bm.capabilities>
        // @returns ListTag
        // @plugin DBetterModel
        // @description
        // Returns the list of capabilities the active compat layer provides on the running BetterModel,
        // for example 'dummy_trackers' or 'animation_signals'. See the BmCapability documentation.
        // -->
        tagProcessor.registerTag(ListTag.class, "capabilities", (attr, obj) -> {
            BmPlatform platform = DBetterModel.platform();
            if (platform == null) return null;
            ListTag list = new ListTag();
            for (BmCapability capability : platform.capabilities()) {
                list.addObject(new ElementTag(CoreUtilities.toLowerCase(capability.name())));
            }
            return list;
        });

        // <--[tag]
        // @attribute <bm.models>
        // @returns ListTag
        // @plugin DBetterModel
        // @description
        // Returns the names of all models registered in BetterModel.
        // -->
        tagProcessor.registerTag(ListTag.class, "models", (attr, obj) -> {
            BmPlatform platform = DBetterModel.platform();
            return platform == null ? null : new ListTag(new java.util.ArrayList<>(platform.modelNames()));
        });

        // <--[tag]
        // @attribute <bm.limbs>
        // @returns ListTag
        // @plugin DBetterModel
        // @description
        // Returns the names of all player limb models registered in BetterModel.
        // -->
        tagProcessor.registerTag(ListTag.class, "limbs", (attr, obj) -> {
            BmPlatform platform = DBetterModel.platform();
            return platform == null ? null : new ListTag(new java.util.ArrayList<>(platform.limbNames()));
        });
    }
}

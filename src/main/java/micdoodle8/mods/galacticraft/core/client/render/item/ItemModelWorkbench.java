package micdoodle8.mods.galacticraft.core.client.render.item;

import micdoodle8.mods.galacticraft.core.Constants;
import micdoodle8.mods.galacticraft.core.wrappers.ModelTransformWrapper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

public class ItemModelWorkbench extends ModelTransformWrapper
{
    public ItemModelWorkbench(IBakedModel modelToWrap)
    {
        super(modelToWrap);
    }

    @Override
    protected Matrix4f getTransformForPerspective(TransformType cameraTransformType)
    {
        if (cameraTransformType == TransformType.GUI)
        {
            Matrix4f ret = new Matrix4f();
            ret.setIdentity();
            Matrix4f mul = new Matrix4f();
            mul.setIdentity();
            mul.setTranslation(new Vector3f(0.0F, -0.0F, 0.0F));
            ret.mul(mul);
            mul.setIdentity();
            mul.setScale(1.0F);
            ret.mul(mul);
            return ret;
        }

        if (cameraTransformType == TransformType.THIRD_PERSON_RIGHT_HAND)
        {
            Vector3f trans = new Vector3f(0.0F, 0.5F, 0.0F);
            Matrix4f ret = new Matrix4f();
            ret.setIdentity();
            Matrix4f mul = new Matrix4f();
            mul.setIdentity();
            mul.setScale(0.35F);
            ret.mul(mul);
            mul.setIdentity();
            mul.rotX(- Constants.halfPI);
            ret.mul(mul);
            mul.setIdentity();
            mul.setTranslation(trans);
            ret.mul(mul);
            return ret;
        }

        return null;
    }
}

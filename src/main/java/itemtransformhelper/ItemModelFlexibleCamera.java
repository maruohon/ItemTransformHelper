package itemtransformhelper;


import java.util.List;

import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import net.minecraftforge.client.model.Attributes;
import net.minecraftforge.client.model.IFlexibleBakedModel;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.client.model.ISmartBlockModel;
import net.minecraftforge.client.model.ISmartItemModel;
import net.minecraftforge.client.model.TRSRTransformation;

/**
 * User: The Grey Ghost
 * Date: 20/01/2015
 * This class is a simple wrapper to substitute a new set of camera transforms for an existing item
 * Usage:
 * 1) Construct a new ItemModelFlexibleCamera with the model to wrap and an UpdateLink using getWrappedModel()
 * 2) Replace the ItemModelFlexibleCamera into the modelRegistry in place of the model to wrap
 * 3) Alter the UpdateLink to control the ItemCameraTransform of a given model:
 *   a) itemModelToOverride selects the item to be overridden
 *   b) forcedTransform is the transform to apply
 * Models which don't match itemModelToOverride will use their original transform
 *
 * The wrapper currently understands IFlexibleBakedModel, ISmartItemModel, ISmartBlockModel, and IPerspectiveAwareModel
 *
 * NB Starting with Forge 1.8-11.14.4.1563, it appears that all items now implement IPerspectiveAwareModel
 */
@SuppressWarnings({ "deprecation" })
public class ItemModelFlexibleCamera implements IFlexibleBakedModel, ISmartItemModel, ISmartBlockModel
{
  public static ItemModelFlexibleCamera getWrappedModel(IBakedModel modelToWrap, UpdateLink linkToCurrentInformation)
  {
    if (modelToWrap instanceof IPerspectiveAwareModel) {
      return new ItemModelFlexibleCameraPerspectiveAware(modelToWrap, linkToCurrentInformation);
    } else {
      return new ItemModelFlexibleCamera(modelToWrap, linkToCurrentInformation);
    }
  }

  private ItemModelFlexibleCamera(IBakedModel i_modelToWrap, UpdateLink linkToCurrentInformation)
  {
    updateLink = linkToCurrentInformation;
    iBakedModel = i_modelToWrap;
  }

  @Override
  public List<BakedQuad> getFaceQuads(EnumFacing enumFacing) {
    return iBakedModel.getFaceQuads(enumFacing);
  }

  @Override
  public List<BakedQuad> getGeneralQuads() {
    return iBakedModel.getGeneralQuads();
  }

  @Override
  public VertexFormat getFormat() {
    if (iBakedModel instanceof IFlexibleBakedModel) {
      return ((IFlexibleBakedModel) iBakedModel).getFormat();
    } else {
      return Attributes.DEFAULT_BAKED_FORMAT;
    }
  }

  @Override
  public boolean isAmbientOcclusion() {
    return iBakedModel.isAmbientOcclusion();
  }

  @Override
  public boolean isGui3d() {
    return iBakedModel.isGui3d();
  }

  @Override
  public boolean isBuiltInRenderer() {
    return iBakedModel.isBuiltInRenderer();
  }

  @Override
  public TextureAtlasSprite getParticleTexture()
  {
    return iBakedModel.getParticleTexture();
  }

  @Override
  public ItemCameraTransforms getItemCameraTransforms() {
    return (updateLink.itemModelToOverride == this) ? updateLink.forcedTransform : iBakedModel.getItemCameraTransforms();
  }

  protected final UpdateLink updateLink;

  public IBakedModel getIBakedModel() {
    return iBakedModel;
  }

  private final IBakedModel iBakedModel;

  @Override
  public IBakedModel handleItemState(ItemStack stack) {
    if (iBakedModel instanceof ISmartItemModel) {
      IBakedModel baseModel = ((ISmartItemModel)iBakedModel).handleItemState(stack);
      return new ItemModelFlexibleCamera(baseModel, updateLink);
    } else {
      return this;
    }
  }

  @Override
  public IBakedModel handleBlockState(IBlockState state) {
    if (iBakedModel instanceof ISmartBlockModel) {
      IBakedModel baseModel = ((ISmartBlockModel)iBakedModel).handleBlockState(state);
      return new ItemModelFlexibleCamera(baseModel, updateLink);
    } else {
      return this;
    }
  }

  public static class ItemModelFlexibleCameraPerspectiveAware extends ItemModelFlexibleCamera implements IPerspectiveAwareModel
  {
    ItemModelFlexibleCameraPerspectiveAware(IBakedModel i_modelToWrap, UpdateLink linkToCurrentInformation)
    {
      super(i_modelToWrap, linkToCurrentInformation);
    }

    @Override
    public Pair<? extends IFlexibleBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
      if (updateLink.itemModelToOverride == this) {
        ItemTransformVec3f itemTransformVec3f;
        switch (cameraTransformType) {
            case NONE: {
                itemTransformVec3f = ItemTransformVec3f.DEFAULT;
                break;
            }
            case FIRST_PERSON: {
                itemTransformVec3f = updateLink.forcedTransform.firstPerson;
                break;
            }
            case THIRD_PERSON: {
                itemTransformVec3f = updateLink.forcedTransform.thirdPerson;
                break;
            }
            case GUI: {
                itemTransformVec3f = updateLink.forcedTransform.gui;
                break;
            }
            case HEAD: {
                itemTransformVec3f = updateLink.forcedTransform.head;
                break;
            }
            case GROUND: {
                itemTransformVec3f = updateLink.forcedTransform.ground;
                break;
            }
            case FIXED: {
                itemTransformVec3f = updateLink.forcedTransform.fixed;
                break;
            }
            default: {
             throw new IllegalArgumentException("Unknown cameraTransformType:" + cameraTransformType);
            }
        }

        TRSRTransformation tr = new TRSRTransformation(itemTransformVec3f);
        Matrix4f mat = null;
        if (tr != null && tr != TRSRTransformation.identity()) {
            mat = TRSRTransformation.blockCornerToCenter(tr).getMatrix();
        }

        return Pair.of((IFlexibleBakedModel)this, mat);
      } else {
        IBakedModel baseModel = getIBakedModel();
        return ((IPerspectiveAwareModel) baseModel).handlePerspective(cameraTransformType);
      }
    }
  }

  public static class UpdateLink
  {
    public IBakedModel itemModelToOverride;
    public ItemCameraTransforms forcedTransform;
  }

}

package org.talend.designer.business.model.business.diagram.part;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.DelegatingLayout;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramRootEditPart;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.ide.document.StorageDiagramDocumentProvider;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.ide.editor.FileDiagramEditor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.properties.BusinessProcessItem;
import org.talend.core.model.properties.Item;
import org.talend.designer.business.diagram.custom.actions.DiagramResourceManager;
import org.talend.designer.business.diagram.custom.dnd.BusinessDiagramDropTargetListener;
import org.talend.designer.business.diagram.custom.properties.BusinessPropertiesBrowserPage;
import org.talend.designer.business.model.business.diagram.edit.parts.BusinessEditPartFactory;
import org.talend.repository.editor.RepositoryEditorInput;
import org.talend.repository.model.IRepositoryFactory;
import org.talend.repository.model.RepositoryFactoryProvider;
import org.talend.repository.ui.views.IRepositoryView;
import org.talend.repository.ui.views.RepositoryView;

/**
 * @generated
 */
public class BusinessDiagramEditor extends FileDiagramEditor implements IGotoMarker {

    /**
     * @generated
     */
    public static final String ID = "org.talend.designer.business.model.business.diagram.part.BusinessDiagramEditorID"; //$NON-NLS-1$

    /**
     * @generated
     */
    public BusinessDiagramEditor() {
        super(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.gmf.runtime.diagram.ui.resources.editor.parts.DiagramDocumentEditor#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(Class type) {
        if (type == IPropertySheetPage.class) {
            return new BusinessPropertiesBrowserPage(this);
        }
        return super.getAdapter(type);
    }

    /**
     * @generated
     */
    protected String getEditingDomainID() {
        return "org.talend.designer.business.diagram.EditingDomain"; //$NON-NLS-1$
    }

    /**
     * @generated
     */
    protected TransactionalEditingDomain createEditingDomain() {
        TransactionalEditingDomain domain = super.createEditingDomain();
        domain.setID(getEditingDomainID());
        return domain;
    }

    /**
     * @generated
     */
    protected void setDocumentProvider(IEditorInput input) {
        if (input.getAdapter(IFile.class) != null) {
            setDocumentProvider(new BusinessDocumentProvider());
        } else {
            setDocumentProvider(new StorageDiagramDocumentProvider());
        }
    }

    /**
     * @generated
     */
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        DiagramRootEditPart root = (DiagramRootEditPart) getDiagramGraphicalViewer().getRootEditPart();
        LayeredPane printableLayers = (LayeredPane) root.getLayer(LayerConstants.PRINTABLE_LAYERS);
        FreeformLayer extLabelsLayer = new FreeformLayer();
        extLabelsLayer.setLayoutManager(new DelegatingLayout());
        printableLayers.addLayerAfter(extLabelsLayer, BusinessEditPartFactory.EXTERNAL_NODE_LABELS_LAYER,
                LayerConstants.PRIMARY_LAYER);
        LayeredPane scalableLayers = (LayeredPane) root.getLayer(LayerConstants.SCALABLE_LAYERS);
        FreeformLayer scaledFeedbackLayer = new FreeformLayer();
        scaledFeedbackLayer.setEnabled(false);
        scalableLayers.addLayerAfter(scaledFeedbackLayer, LayerConstants.SCALED_FEEDBACK_LAYER,
                DiagramRootEditPart.DECORATION_UNPRINTABLE_LAYER);
    }

    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        getDiagramGraphicalViewer().addDropTargetListener(new BusinessDiagramDropTargetListener(getDiagramGraphicalViewer()));
    }

    RepositoryEditorInput repositoryEditorInput;

    public void setInput(IEditorInput input) {
        super.setInput(input);
        if (input instanceof RepositoryEditorInput) {
            repositoryEditorInput = (RepositoryEditorInput) input;
        }
    }

    public void doSave(IProgressMonitor progressMonitor) {
        super.doSave(progressMonitor);
        if (repositoryEditorInput != null) {
            DiagramResourceManager diagramResourceManager = new DiagramResourceManager();
            BusinessProcessItem businessProcessItem = (BusinessProcessItem) repositoryEditorInput.getItem();
            diagramResourceManager.updateFromResource(businessProcessItem, repositoryEditorInput.getFile());

            try {
                RepositoryFactoryProvider.getInstance().save(businessProcessItem);
            } catch (PersistenceException e) {
                e.printStackTrace();
            }
            propertyIsDirty = false;
            firePropertyChange(IEditorPart.PROP_DIRTY);
            IRepositoryView viewPart = (IRepositoryView) getSite().getPage().findView(RepositoryView.VIEW_ID);
            viewPart.refresh();
            
        }
    }

    public boolean isEditable() {
        if (repositoryEditorInput != null) {
            Item item = repositoryEditorInput.getItem();
            IRepositoryFactory repositoryFactory = RepositoryFactoryProvider.getInstance();
            boolean locked = false;
            boolean deleted = false;
            try {
                locked = repositoryFactory.isLocked(item);
                deleted = repositoryFactory.isDeleted(item);
            } catch (PersistenceException e) {
                e.printStackTrace();
            }
            return !deleted;
        }
        return super.isEditable();
    }

    @Override
    public boolean isDirty() {
        return propertyIsDirty || super.isDirty();
    }

    public void dispose() {
        super.dispose();
        // Unlock the process :
        IRepositoryFactory repFactory = RepositoryFactoryProvider.getInstance();
        try {
            repFactory.unlock(repositoryEditorInput.getItem());
            repFactory.reload(repositoryEditorInput.getItem().getProperty()); 
            
        } catch (PersistenceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        IRepositoryView viewPart = (IRepositoryView) getSite().getPage().findView(RepositoryView.VIEW_ID);
        viewPart.refresh();
    }
    
    public void init(final IEditorSite site, final IEditorInput editorInput) throws PartInitException {
        super.init(site, editorInput);
        // Lock the process :
        IRepositoryFactory repFactory = RepositoryFactoryProvider.getInstance();
        try {
            RepositoryEditorInput processEditorInput = (RepositoryEditorInput) editorInput;
            processEditorInput.getItem().getProperty().eAdapters().add(dirtyListener);
            boolean deleted = repFactory.isDeleted(processEditorInput.getItem());
            if (!deleted)
                repFactory.lock(processEditorInput.getItem());
        } catch (PersistenceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
    private boolean propertyIsDirty;

    private AdapterImpl dirtyListener = new AdapterImpl() {
        

        public void notifyChanged(Notification notification) {
            if (notification.getEventType() != Notification.REMOVING_ADAPTER)
            {
                propertyIsDirty = true;
                firePropertyChange(IEditorPart.PROP_DIRTY);
            }
        }
    };


}

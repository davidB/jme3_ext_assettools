package jme3_ext_spatialexplorer;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;

import org.controlsfx.control.MasterDetailPane;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.control.action.Action;
import org.controlsfx.property.BeanProperty;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class SpatialExplorer {
	TreeItem<Spatial> rootItem = new TreeItem<Spatial>();
	Stage stage;
	PropertySheet details;
	ActionShowInPropertySheet selectAction;
	public final ObjectProperty<Spatial> selection = new SimpleObjectProperty<>();

	public void start(Stage primaryStage) {
		stop();
		this.stage = primaryStage;
		primaryStage.setTitle("Spatial Explorer");

		details = new PropertySheet();

		selectAction = new ActionShowInPropertySheet("test", null, details);

		TreeView<Spatial> tree = new TreeView<>(rootItem);
		tree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		//		tree.setCellFactory((treeview) -> new MyTreeCell());
		tree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue)  -> {
			try {
				select(newValue.getValue());
				Platform.runLater(()->{
					update(newValue.getValue(), newValue);
				});

			} catch(Exception exc){
				exc.printStackTrace();
			}
		});

		//StackPane root = new StackPane();
		MasterDetailPane pane = new MasterDetailPane();
		pane.setMasterNode(tree);
		pane.setDetailNode(details);
		pane.setDetailSide(Side.RIGHT);
		pane.setShowDetailNode(true);
		primaryStage.setScene(new Scene(pane, 400, 500));
		primaryStage.show();
	}

	public void stop() {
		if (stage != null) {
			updateRoot(null);
			stage.hide();
			stage.close();
			stage = null;
		}
	}

	public void updateRoot(Spatial value) {
		update(value, rootItem);
		rootItem.setExpanded(value != null);
	}

	public void select(Spatial value) {
		System.out.println("selected : " + value);
		for (String key : value.getUserDataKeys()) {
			System.out.printf("\tusedata : %s -> %s \n", key, value.getUserData(key));
		}
		selectAction.bean = value;
		selectAction.handle(null);
		selection.set(value);
	}

	void update(Spatial value, TreeItem<Spatial> item) {
		item.setValue(value);
		item.getChildren().clear();
		if (value == null) return;

		if (value instanceof Node) {
			for (Spatial child : ((Node)value).getChildren()) {
				TreeItem<Spatial> childItem = new TreeItem<Spatial>();
				update(child, childItem);
				item.getChildren().add(childItem);
			}
		}
//		item.expandedProperty().addListener((evt) -> {
//			if (((BooleanProperty)evt).get()) {
//				update(value, item);
//			}
//		});
	}

//	class MyTreeCell extends TextFieldTreeCell<Spatial> {
//
//		public MyTreeCell() {
//			editableProperty().set(false);
//		}
//
//		@Override
//		public void updateSelected(boolean v) {
//			super.updateSelected(v);
//			if (v) {
//				SpatialExplorer.this.update(getTreeItem().getValue(), getTreeItem());
//				SpatialExplorer.this.select(getTreeItem().getValue());
//			}
//		}
//		@Override
//		public void updateItem(Spatial item, boolean empty) {
//			super.updateItem(item, empty);
//
//			if (empty) {
//				setText(null);
//				setGraphic(null);
//			} else {
//				setText(String.valueOf(getTreeItem().getValue()));
//				setGraphic(getTreeItem().getGraphic());
//				//                if (
//				//                    !getTreeItem().isLeaf()&&getTreeItem().getParent()!= null
//				//                ){
//				//                    setContextMenu(addMenu);
//				//                }
//			}
//		}
//	}
}

class ActionShowInPropertySheet extends Action {

	Object bean;
	final PropertySheet propertySheet;

	public ActionShowInPropertySheet(String title, Object bean, PropertySheet propertySheet) {
		super(title);
		setEventHandler(this::handleAction);
		this.bean = bean;
		this.propertySheet = propertySheet;
	}

	private ObservableList<Item> getCustomModelProperties() {
		ObservableList<Item> list = FXCollections.observableArrayList();
		//        for (String key : customDataMap.keySet()) {
		//            list.add(new CustomPropertyItem(key));
		//        }
		return list;
	}

	private void handleAction(ActionEvent ae) {

		// retrieving bean properties may take some time
		// so we have to put it on separate thread to keep UI responsive
		Service<?> service = new Service<ObservableList<Item>>() {

			@Override
			protected Task<ObservableList<Item>> createTask() {
				return new Task<ObservableList<Item>>() {
					@Override
					protected ObservableList<Item> call() throws Exception {
						return bean == null ? getCustomModelProperties() : getProperties(bean);
					}
				};
			}

		};
		service.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

			@SuppressWarnings("unchecked")
			@Override
			public void handle(WorkerStateEvent e) {
				//                if (bean instanceof SampleBean) {
				//                    for (Item i : (ObservableList<Item>) e.getSource().getValue()) {
				//                        if (i instanceof BeanProperty && ((BeanProperty) i).getPropertyDescriptor() instanceof CustomPropertyDescriptor) {
				//                            BeanProperty bi = (BeanProperty) i;
				//                            bi.setEditable(((CustomPropertyDescriptor) bi.getPropertyDescriptor()).isEditable());
				//                        }
				//                    }
				//                }
				propertySheet.getItems().setAll((ObservableList<Item>) e.getSource().getValue());
			}
		});
		service.start();

	}

	/**
	 * Given a JavaBean, this method will return a list of {@link Item} intances,
	 * which may be directly placed inside a {@link PropertySheet} (via its
	 * {@link PropertySheet#getItems() items list}.
	 *
	 * @param bean The JavaBean that should be introspected and be editable via
	 *      a {@link PropertySheet}.
	 * @return A list of {@link Item} instances representing the properties of the
	 *      JavaBean.
	 */
	public static ObservableList<Item> getProperties(final Object bean) {
		ObservableList<Item> list = FXCollections.observableArrayList();

		try {
			BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass(), Object.class);
			for (PropertyDescriptor p : beanInfo.getPropertyDescriptors()) {
				if (isProperty(p) && ! p.isHidden() && p.getReadMethod() != null) {
					BeanProperty bp = new BeanProperty(bean, p);
					bp.setEditable(false);
					list.add(bp);
				}
			}
		} catch (IntrospectionException e) {
			e.printStackTrace();
		}

		return list;
	}

	private static boolean isProperty(final PropertyDescriptor p) {
		//TODO  Add more filtering
		return p.getWriteMethod() != null && !p.getPropertyType().isAssignableFrom(EventHandler.class);
	}

}
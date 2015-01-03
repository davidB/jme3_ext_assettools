package jme3_ext_spatial_explorer;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.LinkedList;
import java.util.List;

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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;

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

	/** List of actions on TreeItem via ContextMenu. actions should be registered before start(). */
	public final List<Action> actions = new LinkedList<>();

	MasterDetailPane makePane() {
		details = new PropertySheet();

		selectAction = new ActionShowInPropertySheet("test", null, details);

		TreeView<Spatial> tree = new TreeView<>(rootItem);
		tree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		tree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue)  -> {
			try {
				if (newValue == null) {
					select(null);
				} else {
					select(newValue.getValue());
				}
			} catch(Exception exc){
				exc.printStackTrace();
			}
		});

		tree.setCellFactory((treeview) -> new MyTreeCell());

		//StackPane root = new StackPane();
		MasterDetailPane pane = new MasterDetailPane();
		pane.setMasterNode(tree);
		pane.setDetailNode(details);
		pane.setDetailSide(Side.RIGHT);
		pane.setDividerPosition(0.5);
		pane.setShowDetailNode(true);
		return pane;
	}

	public void start(Stage primaryStage) {
		stop();
		this.stage = primaryStage;
		primaryStage.setTitle("Spatial Explorer");
		primaryStage.setScene(new Scene(makePane(), 600, 500));
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
		selectAction.select(value);
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
	}

	class MyTreeCell extends TextFieldTreeCell<Spatial> {
		private ContextMenu menu = new ContextMenu();

		public MyTreeCell() {
			editableProperty().set(false);
			setContextMenu(menu);
			for (Action a : actions) {
				MenuItem mi = new MenuItem(a.getText());
				mi.setOnAction((evt) -> {a.handle(new ActionEvent(getTreeItem(), evt.getTarget()));});
				menu.getItems().add(mi);
			}
		}
	}
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

	public void select(Spatial v) {
		bean = v;
		handle(null);
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
						return bean == null ? null : getProperties(bean);
					}
				};
			}

		};
		service.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

			@SuppressWarnings("unchecked")
			@Override
			public void handle(WorkerStateEvent e) {
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
			if (bean instanceof Spatial) {
				Spatial sp = (Spatial)bean;
				for(String key : sp.getUserDataKeys()) {
					list.add(new BasicItem("UserData", key, sp.getUserData(key)));
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

	@RequiredArgsConstructor
	static class BasicItem implements PropertySheet.Item {
		final String category;
		final String name;
		final Object value;

		@Override
		public void setValue(Object value) {
		}

		@Override
		public Object getValue() {
			return value;
		}

		@Override
		public Class<?> getType() {
			return getValue().getClass();
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public String getCategory() {
			return category;
		}

		/** {@inheritDoc} */
		@Override
		public boolean isEditable() {
			return false;
		}
	}
}
package hu.pizzavalto.pizzaproject.fragments;

import static android.view.View.GONE;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import hu.pizzavalto.pizzaproject.R;
import hu.pizzavalto.pizzaproject.model.Pizza;
import hu.pizzavalto.pizzaproject.model.PizzaViewModel;

/**
 * A simple {@link Fragment} subclass.
 */
public class CartFragment extends Fragment {

    private PizzaViewModel pizzaViewModel;
    private LinearLayout itemContainer;

    public CartFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pizzaViewModel = new ViewModelProvider(requireActivity()).get(PizzaViewModel.class);
    }


    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_cart, container, false);
        itemContainer = view.findViewById(R.id.itemContainer);

        MenuFragment menuFragment = new MenuFragment();
        menuFragment.setTargetFragment(CartFragment.this, 0);

        HashMap<Long, Integer> pizzaIds = pizzaViewModel.getPizzaIds();

        TextView messageText = new TextView(getActivity());
        if (pizzaIds.isEmpty()) {
            messageText.setVisibility(View.VISIBLE);
            messageText.setText("Nincs semmi a kosaradban.");
            itemContainer.addView(messageText);
        } else {
            messageText.setVisibility(GONE);
            itemContainer.removeView(messageText);
            for (Long pizzaId: pizzaIds.keySet()) {
                Optional<Integer> pizzaIdValue = Optional.ofNullable(pizzaIds.get(pizzaId));
                if (pizzaIdValue.isPresent()) {
                    final int[] count = { pizzaIdValue.get() };
                    Pizza pizza = pizzaViewModel.getPizzas().stream().filter((x) -> Objects.equals(x.getId(), pizzaId)).findFirst().get();

                    View cartItem = LayoutInflater.from(getActivity()).inflate(R.layout.cart_item, (ViewGroup) getView(), false);

                    TextView pizzaName = cartItem.findViewById(R.id.pizzaName);
                    pizzaName.setText(pizza.getName());

                    TextView itemQuantity = cartItem.findViewById(R.id.itemQuantity);
                    if (count[0] < 2){
                        itemQuantity.setText("");
                    }
                    itemQuantity.setText(String.valueOf(count[0]));
                    itemContainer.addView(cartItem);

                    TextView sumPrice = cartItem.findViewById(R.id.sumPrice);
                    sumPrice.setText(setPriceText(count[0], pizza.getPrice()));

                    Button minusButton = cartItem.findViewById(R.id.button_minus);
                    Button plusButton = cartItem.findViewById(R.id.button_plus);
                    Button deleteButton = cartItem.findViewById(R.id.deleteItem);

                    minusButton.setOnClickListener(decrease -> {
                        if (count[0] > 1) {
                            count[0]--;
                            pizzaIds.put(pizzaId, count[0]);

                            itemQuantity.setText(String.valueOf(count[0]));
                            sumPrice.setText(setPriceText(count[0], pizza.getPrice()));
                        }
                    });

                    plusButton.setOnClickListener(increase -> {
                        count[0]++;
                        pizzaIds.put(pizzaId, count[0]);

                        itemQuantity.setText(String.valueOf(count[0]));
                        sumPrice.setText(setPriceText(count[0], pizza.getPrice()));
                    });

                    deleteButton.setOnClickListener(increase -> {
                        itemContainer.removeView(cartItem);
                        pizzaIds.remove(pizza.getId());
                        pizzaViewModel.setPizzaIds(pizzaIds);
                        pizzaViewModel.getPizzas().remove(pizza);
                    });
                } else {
                    // handle the null case
                    System.out.println("nullgec");
                }
            }
        }
        return view;
    }


    public String setPriceText(int count, int price){
        return String.valueOf(count * price);
    }
}
(ns widgetshop.main
  "Main entrypoint for the widgetshop frontend."
  (:require [reagent.core :as r]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core :refer [get-mui-theme color]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [widgetshop.app.state :as state]
            [widgetshop.app.products :as products]
            [widgetshop.routes :as routes]))



;; Task 1: refactor this, the listing of products in a category should
;; be its own component (perhaps in another namespace).
;;
;; Task 2: Add actions to add item to cart. See that cart badge is automatically updated.
;;

(defn product-view [product]
  [:span
   [:h1 (:name product)]
   [:h2 (str (:price product) "€")]
   [:p (:description product)]
   [ui/flat-button {:on-click #(do (products/unselect-product!)
                                   (products/set-page! :product-listing))} "Back"]
   [ui/flat-button {:on-click #(products/add-review!)} "Rate this product"]
   (when (:review product)
     [:div
      [ui/text-field {:id "review-comment"
                      :floating-label-text "Review text"
                      :value (get-in product [:review :comment])
                      :on-change (fn [event value]
                                   (products/update-review-comment! value))}]
      (doall
       (for [rating (range 1 6)]
        (if (<= rating (get-in product [:review :stars]))
          ^{:key rating} [ic/toggle-star {:on-click #(products/update-review-star-rating! rating)}]
          ^{:key rating} [ic/toggle-star-border {:on-click #(products/update-review-star-rating! rating)}])))
      [ui/flat-button {:primary true
                       :on-click products/submit-review!} "Save review"]])])

(defn product-listing [products]
  ;; Product listing for the selected category
  (if (= :loading products)
    [ui/refresh-indicator {:status "loading" :size 40 :left 10 :top 10}]

    [ui/table {:on-row-selection #(do (products/select-product! (first (js->clj %)))
                                      (products/set-page! :product-page))}
     [ui/table-header {:display-select-all false :adjust-for-checkbox false}
      [ui/table-row
       [ui/table-header-column "Name"]
       [ui/table-header-column "Description"]
       [ui/table-header-column "Price (€)"]
       [ui/table-header-column "Average rating"]
       [ui/table-header-column "Add to cart"]]]
     [ui/table-body {:display-row-checkbox false}
      (for [{:keys [id name description price stars] :as product} products]
        ^{:key id}
        [ui/table-row
         [ui/table-row-column name]
         [ui/table-row-column description]
         [ui/table-row-column price]
         [ui/table-row-column (when-not (= stars 0)
                                (str (.toFixed stars 2) "/5"))]
         [ui/table-row-column
          [ui/flat-button {:primary true :on-click #(do
                                                      (.stopPropagation %)
                                                      (products/add-to-cart! product))}
           "Add to cart"]]])]]))

(defn cart-listing
  ([products]
   (cart-listing products true))
  ([products editable?]
   [ui/table
    [ui/table-header {:display-select-all false :adjust-for-checkbox false}
     [ui/table-row
      [ui/table-header-column "Name"]
      [ui/table-header-column "Description"]
      [ui/table-header-column "Price (€)"]
      [ui/table-header-column "Quantity"]
      [ui/table-header-column "Total"]
      (when editable?
        [ui/table-header-column])]]
    [ui/table-body {:display-row-checkbox false}
     (for [{:keys [id name description price stars] :as product} (keys products)]
       (let [product-count (get products product)]
         ^{:key id}
         [ui/table-row
          [ui/table-row-column name]
          [ui/table-row-column description]
          [ui/table-row-column price]
          [ui/table-row-column [:input {:type "number"
                                        :value product-count
                                        :on-change #(products/update-amount-in-cart! product (-> % .-target .-value))
                                        :disabled (not editable?)}]]
          [ui/table-row-column (str (* price product-count) "€")]
          (when editable?
           [ui/table-row-column [:button {:on-click #(products/remove-from-cart! product)} "muffinssi"]])]))]]))

(defn checkout-navigation-buttons [checkout-info]
  [:span
   [ui/flat-button {:disabled (zero? (:step checkout-info))
                    :on-click #(products/update-checkout-step! dec)} "Back"]
   [ui/raised-button {:primary true
                      :on-click #(products/update-checkout-step! inc)} "Next"]])

(defn general-form [header fields]
  [:div
   [:h1 header]
   (doall
     (for [field fields]
       ^{:key (:label field)}
       [:span
        [ui/text-field {:id (:label field)
                        :floating-label-text (:label field)
                        :value (:value field)
                        :on-change (:on-change field)}]
        [:br]]))])

(defn confirmation-page [cart checkout-info]
  [:div
   [:h1 "Confirm your order"]
   [:h2 "Payment details"]
   [:p "Name: " (get-in checkout-info [:billing :name])]
   [:p "Card number: " (get-in checkout-info [:billing :card-number])]
   [:h2 "Shipping details"]
   [:p "Recipient: " (get-in checkout-info [:shipping :name])]
   [:p "Shipping address: " (get-in checkout-info [:shipping :address])]
   [:h2 "Items in order "]
   [cart-listing cart false]])

(defn checkout [cart checkout-info]
  [:div
   [ui/stepper {:active-step (:step checkout-info)}
    [ui/step
     [ui/step-label "Payment method"]]
    [ui/step
     [ui/step-label "Delivery info"]]
    [ui/step
     [ui/step-label "Confirm your order"]]
    [ui/step
     [ui/step-label "Thank you"]]]
   (case (:step checkout-info)
     0 [general-form "Payment info " [{:label "Name"
                                       :value (get-in checkout-info [:billing :name])
                                       :on-change (fn [event value]
                                                    (products/update-billing-name! value))}
                                      {:label "Card Number"
                                       :value (get-in checkout-info [:billing :card-number])
                                       :on-change (fn [event value]
                                                    (products/update-billing-card-number! value))}]]
     1 [general-form "Delivery info" [{:label "Recipient name"
                                       :value (get-in checkout-info [:shipping :name])
                                       :on-change (fn [event value]
                                                    (products/update-shipping-name! value))}
                                      {:label "Delivery address"
                                       :value (get-in checkout-info [:shipping :address])
                                       :on-change (fn [event value]
                                                    (products/update-shipping-address! value))}]]
     2 [confirmation-page cart checkout-info]
     3 [:h1 "Thank you for your order!"])
   (when (< (:step checkout-info) 3)
     [checkout-navigation-buttons checkout-info])])

(defn shopping-cart [cart]
  [:div
   [:h1 "Shopping cart"]
   [cart-listing cart]
   [ui/flat-button {:primary true
                    :on-click #(products/set-page! :product-listing)} "Back to product listing"]
   [ui/flat-button {:primary true
                    :on-click #(products/set-page! :checkout)} "Checkout"]])

(defn widgetshop [app]
  [ui/mui-theme-provider
   {:mui-theme (get-mui-theme
                {:palette {:text-color (color :green600)}})}
   [:div
    [ui/app-bar {:title "Widgetshop!"
                 :icon-element-right
                 (r/as-element [ui/badge {:badge-content (reduce + (vals (:cart app)))
                                          :badge-style {:top 12 :right 12}}
                                [ui/icon-button {:tooltip "Checkout"
                                                 :on-click #(products/set-page! :cart)}
                                 (ic/action-shopping-cart)]])}]
    [ui/snackbar {:auto-hide-duration 5000
                  :on-request-close products/remove-alert!
                  :open (boolean (:alert app))
                  :message (:alert app)}]
    [ui/paper
     (case (:page app)
       (:front-page :product-listing)
       [:span
        (when-not (= :loading (:categories app))
          [ui/select-field {:floating-label-text "Select product category"
                            :value (:id (:category app))
                            :on-change (fn [evt idx value]
                                         (products/select-category-by-id! value))}
           (for [{:keys [id name] :as category} (:categories app)]
             ^{:key id}
             [ui/menu-item {:value id :primary-text name}])])
        [product-listing ((:products-by-category app) (:category app))]]
       :product-page [product-view (some #(when (= (:id %) (:selected-item-id app) %)) (get (:products-by-category app) (:category app)))]
       :cart [shopping-cart (:cart app)]
       :checkout [checkout (:cart app) (:checkout app)])]]])


(defn main-component []
  [widgetshop @state/app])

(defn ^:export main []
  (products/load-product-categories! routes/start!)
  (r/render-component [main-component] (.getElementById js/document "app")))

(defn ^:export reload-hook []
  (r/force-update-all))

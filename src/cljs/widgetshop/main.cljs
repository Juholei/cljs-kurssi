(ns widgetshop.main
  "Main entrypoint for the widgetshop frontend."
  (:require [reagent.core :as r]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core :refer [get-mui-theme color]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [widgetshop.app.state :as state]
            [widgetshop.app.products :as products]))



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
   [ui/flat-button {:on-click #(products/unselect-product!)} "Back"]
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

    [ui/table {:on-row-selection #(products/select-product! (first (js->clj %)))}
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

(defn widgetshop [app]
  [ui/mui-theme-provider
   {:mui-theme (get-mui-theme
                {:palette {:text-color (color :green600)}})}
   [:div
    [ui/app-bar {:title "Widgetshop!"
                 :icon-element-right
                 (r/as-element [ui/badge {:badge-content (count (:cart app))
                                          :badge-style {:top 12 :right 12}}
                                [ui/icon-button {:tooltip "Checkout"}
                                 (ic/action-shopping-cart)]])}]
    [ui/snackbar {:auto-hide-duration 5000
                  :on-request-close products/remove-alert!
                  :open (:alert app)
                  :message (:alert app)}]
    [ui/paper

     (if (:selected-item app)
       [product-view (:selected-item app)]
       ;; Product category selection
       [:span
        (when-not (= :loading (:categories app))
          [ui/select-field {:floating-label-text "Select product category"
                            :value (:id (:category app))
                            :on-change (fn [evt idx value]
                                         (products/select-category-by-id! value))}
           (for [{:keys [id name] :as category} (:categories app)]
             ^{:key id}
             [ui/menu-item {:value id :primary-text name}])])
        [product-listing ((:products-by-category app) (:category app))]])]]])

(defn main-component []
  [widgetshop @state/app])

(defn ^:export main []
  (products/load-product-categories!)
  (r/render-component [main-component] (.getElementById js/document "app")))

(defn ^:export reload-hook []
  (r/force-update-all))

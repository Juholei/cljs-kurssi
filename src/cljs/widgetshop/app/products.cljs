(ns widgetshop.app.products
  "Controls product listing information."
  (:require [widgetshop.app.state :as state]
            [widgetshop.server :as server]))

(defn- products-by-category [app category products]
  (assoc-in app [:products-by-category category] products))

(defn- set-categories [app categories]
  (assoc-in app [:categories] categories))

(defn- load-products-by-category! [{:keys [categories] :as app} server-get-fn! category-id]
  (let [category (some #(when (= (:id %) category-id) %) categories)]
    (server-get-fn! category)
    (-> app
        (assoc :category category)
        (assoc-in [:products-by-category category] :loading))))

(defn select-category-by-id! [category-id]
  (state/update-state!
    load-products-by-category!
    (fn [category]
      (server/get! (str "/products/" (:id category))
                   {:on-success #(state/update-state! products-by-category category %)}))
    category-id))

(defn load-product-categories! []
  (server/get! "/categories" {:on-success #(state/update-state! set-categories %)}))

(defn add-to-cart! [product]
  (state/update-state! (fn [app]
                         (update-in app [:cart product] inc))))

(defn remove-from-cart! [product]
  (state/update-state! (fn [app]
                         (update app :cart dissoc product))))

(defn update-amount-in-cart! [product amount]
  (state/update-state! (fn [app]
                         (assoc-in app [:cart product] amount))))

(defn find-product-by-index [app idx]
  (let [category (:category app)
        products-of-category ((:products-by-category app) category)
        product-by-index (nth products-of-category idx)]
    product-by-index))

(defn set-product [app product-idx]
  (assoc app :selected-item (find-product-by-index app product-idx)))

(defn select-product! [product-idx]
  (state/update-state! set-product product-idx))

(defn unselect-product! []
  (state/update-state! dissoc :selected-item))

(defn add-review! []
  (state/update-state! assoc-in [:selected-item :review] {:comment ""
                                                          :stars 0}))

(defn update-review-comment! [comment]
  (state/update-state! assoc-in [:selected-item :review :comment] comment))

(defn update-review-star-rating! [number-of-stars]
  (state/update-state! assoc-in [:selected-item :review :stars] number-of-stars))

(defn set-alert! [alert]
  (state/update-state! assoc :alert alert))

(defn remove-alert! []
  (state/update-state! dissoc :alert))

(defn finish-review-editing! []
  (state/update-state! update :selected-item dissoc :review)
  (set-alert! "Review submitted!"))

(defn submit-review! []
  (state/update-state!
   (fn [app]
     (let [review (assoc (get-in app [:selected-item :review])
                         :product-id (get-in app [:selected-item :id]))]
       (.log js/console "Review: " (pr-str review))
       (server/post! "/review"
                     {:body review
                      :on-success #(finish-review-editing!)
                      :on-failure #(.log js/console "ei onnistunut")})
       (assoc-in app [:selected-item :review :submit-in-progress?] true)))))

(defn set-page! [page]
  (state/update-state! assoc :page page))

(defn update-checkout-step! [update-fn]
  (state/update-state! update-in [:checkout :step] update-fn))

(defn update-billing-name! [name]
  (state/update-state! assoc-in [:checkout :billing :name] name))

(defn update-billing-card-number! [card-number]
  (state/update-state! assoc-in [:checkout :billing :card-number] card-number))

(defn update-shipping-name! [name]
  (state/update-state! assoc-in [:checkout :shipping :name] name))

(defn update-shipping-address! [address]
  (state/update-state! assoc-in [:checkout :shipping :address] address))
